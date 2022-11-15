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

(defn associate [context key associated-context]
  (let [{:keys [primary-id] :as _built-factory} (-> associated-context build-graph/primary entity/factory)
        value-to-assoc (cond-> (->result-meta associated-context)
                         primary-id (get primary-id))]
    (-> context
        (build-graph/associate key associated-context)
        (assoc-value key value-to-assoc))))
