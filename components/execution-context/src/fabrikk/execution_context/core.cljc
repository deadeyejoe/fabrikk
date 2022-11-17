(ns fabrikk.execution-context.core
  (:require [fabrikk.entity.interface :as entity]
            [fabrikk.build-graph.interface :as build-graph]))
;;TODO: Maybe this context should be its own thing rather than sort of half delegating to build-graph?

(defn update-value [{:keys [primary] :as build-graph} f args]
  (update-in build-graph
             [:codex primary]
             entity/update-value #(apply f % args)))

(defn assoc-value [build-graph key value]
  (update-value build-graph assoc [key value]))

(comment
  (assoc-value {:primary 1
                :codex {1 {}
                        2 {}}}
               :foo
               :bar))

(defn ->result [{:keys [primary] :as build-graph}]
  (-> build-graph
      (get-in [:codex primary])
      (entity/value)))

(defn ->result-meta [build-graph]
  (let [bare-entity (->result build-graph)]
    (with-meta bare-entity (assoc build-graph ::meta true))))

(defn meta-result? [x]
  (-> x meta ::meta))

(def identity-association? #{:identity :itself :list-item})

(defn value-to-assoc [result associate-as]
  (cond
    (identity-association? associate-as) result
    (fn? associate-as) (associate-as result)
    (keyword? associate-as) (get result associate-as)
    :else result))

(defn associate [context key associated-context]
  (let [associate-as (or (-> associated-context build-graph/primary entity/associate-as)
                         :itself)
        result (->result-meta associated-context)
        value-to-assoc (value-to-assoc result associate-as)]
    (-> context
        (build-graph/associate key associated-context)
        (assoc-value key value-to-assoc))))

(defn associate-entity [context key entity]
  (let [associate-as (or (entity/associate-as entity)
                         :itself)
        ;; TODO: we should probably use a 'result-meta' here rather than the entity
        ;; value. If it gets assoc'ed as itself it would be nice to be able to then 
        ;; use that value elsewhere
        value-to-assoc (value-to-assoc (entity/value entity) associate-as)]
    (-> context
        (build-graph/add-link key entity)
        (assoc-value key value-to-assoc))))

(defn propagate-edge [build-graph [source label _dest] entity]
  (let [associate-as (or (entity/associate-as entity)
                         :itself)]
    (assoc-in build-graph [:codex source :value label] (value-to-assoc (:value entity) associate-as))))

(defn propagate [{:keys [primary] :as build-graph} {:keys [uuid] :as entity}]
  (if (= primary uuid)
    build-graph ;; The primary is the top of the dag, shouldn't need to propagate any changes
    (reduce (fn [build-graph edge]
              (propagate-edge build-graph edge entity))
            build-graph
            (build-graph/in-edges-with-labels build-graph uuid))))
