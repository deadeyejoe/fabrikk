(ns extrude.execution-context.core
  (:require [extrude.entity.interface :as entity]
            [extrude.build-graph.interface :as build-graph]))

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

(def identity-association? #{:identity :itself})

(defn associate [context key associated-context]
  (let [associate-as (or (-> associated-context build-graph/primary entity/associate-as)
                       :itself)
        result (->result-meta associated-context)
        value-to-assoc (cond 
                         (identity-association? associate-as) result
                         (fn? associate-as) (associate-as result)
                         (keyword? associate-as) (get result associate-as)
                         :else result)]
    (tap> [associate-as result value-to-assoc])
    (-> context
        (build-graph/associate key associated-context)
        (assoc-value key value-to-assoc))))
