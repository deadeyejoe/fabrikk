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
               :bar)
  (update-value {:primary 1
                 :codex {1 {}
                         2 {}}}
                assoc  :foo :bar))

(defn ->result-meta [{:keys [primary] :as build-graph}]
  (let [bare-entity (-> build-graph
                        (get-in [:codex primary])
                        (entity/value))]
    (with-meta bare-entity (assoc build-graph ::meta true))))

(defn meta-result? [x]
  (-> x meta ::meta))
