(ns fabrikk.execution-context.core
  (:require [fabrikk.entity.interface :as entity]
            [fabrikk.build-graph.interface :as build-graph]))
;;TODO: Maybe this context should be its own thing rather than sort of half delegating to build-graph?(

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

(defn assoc-primary-value [build-graph attribute value]
  (build-graph/update-primary-value build-graph assoc attribute value))

(defn assoc-entity-value [build-graph entity-id attribute value]
  (build-graph/update-entity-value build-graph entity-id assoc attribute value))

(defn associate [context attribute associated-context]
  (let [associate-as (or (-> associated-context build-graph/primary entity/associate-as)
                         :itself)
        result (->result-meta associated-context)
        value-to-assoc (value-to-assoc result associate-as)]
    (-> context
        (build-graph/associate [attribute associate-as] associated-context)
        (assoc-primary-value attribute value-to-assoc))))

(defn associate-entity [context attribute entity]
  (let [associate-as (or (entity/associate-as entity) :itself)
        ;; TODO: we should probably use a 'result-meta' here rather than the entity
        ;; value. If it gets assoc'ed as itself it would be nice to be able to then 
        ;; use that value elsewhere
        value (value-to-assoc (entity/value entity) associate-as)]
    (-> context
        (build-graph/add-link [attribute associate-as] entity)
        (assoc-primary-value attribute value))))

(defn propagate-edge [context [source [attribute associate-as] _dest] entity]
  (let [value (value-to-assoc (:value entity) associate-as)]
    (assoc-entity-value context source attribute value)))

(defn propagate [context {:keys [uuid] :as entity}]
  (reduce (fn [context edge]
            (propagate-edge context edge entity))
          context
          (build-graph/in-edges-with-links context uuid)))
