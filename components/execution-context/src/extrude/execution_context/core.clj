(ns extrude.execution-context.core
  (:require [extrude.entity.interface :as entity]
            [extrude.build-graph.interface :as build-graph]))

(defn assoc-value [{:keys [primary] :as build-graph} key value]
  (update-in build-graph
             [:codex primary]
             entity/update-value #(assoc % key value)))

(comment
  (assoc-value {:primary 1
                :codex {1 {}
                        2 {}}}
               :foo
               :bar))

(defn ->result-meta [{:keys [primary] :as build-graph}]
  (with-meta
    (-> build-graph
        (get-in [:codex primary])
        (entity/value))
    build-graph))
