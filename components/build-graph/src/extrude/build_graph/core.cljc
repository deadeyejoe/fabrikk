(ns extrude.build-graph.core
  (:require [extrude.entity.interface :as entity]
            [clojure.spec.alpha :as s]
            [loom.graph :as graph]
            [loom.attr :as attr])
  (:refer-clojure :exclude [refer]))

(s/def ::graph graph/directed?)
(s/def ::primary ::entity/instance)
(s/def ::codex (s/map-of ::entity/uuid ::entity/instance))
(s/def ::instance (s/keys :opt-un [::graph
                                   ::primary
                                   ::codex]))

(defn init []
  {:graph (graph/digraph)
   :codex {}
   :primary nil})

(defn ensure-node [{:keys [codex] :as bg} {:keys [uuid] :as entity}]
  (if-let [existing (get codex uuid)]
    (assoc-in bg [:codex uuid] (entity/combine-no-conflict existing entity))
    (-> bg
        (assoc-in [:codex uuid] entity)
        (update :graph graph/add-nodes uuid))))

(defn set-primary! [bg {:keys [uuid] :as entity}]
  (-> bg
      (ensure-node entity)
      (assoc :primary uuid)))

(defn entity->build-graph [entity]
  (-> (init)
      (ensure-node entity)
      (set-primary! entity)))

(defn link [bg entity-id label other-entity-id]
  (let [edge [entity-id other-entity-id]]
    (-> bg
        (update :graph graph/add-edges edge)
        (update :graph attr/add-attr edge :label label))))

(defn merge-graphs [graph other]
  (-> graph
      (#(apply graph/add-nodes % (graph/nodes other)))
      (#(apply graph/add-edges % (graph/edges other)))))

(comment
  (let [one (graph/digraph [2 1] [4 2])
        two (graph/digraph [4 3] [3 1])]
    [(graph/nodes two)
     (graph/edges two)
     (merge-graphs one two)]))

(defn merge-builds [{:keys [graph codex] :as primary} to-merge]
  (-> primary
      (assoc :codex (merge-with entity/combine-no-conflict codex (:codex to-merge)))
      (assoc :graph (merge-graphs graph (:graph to-merge)))))

(defn associate [primary label {associated-primary :primary :as associated-build-graph}]
  (-> (entity->build-graph primary)
      (merge-builds associated-build-graph)
      (link (:uuid primary) label associated-primary)))

(comment
  (let [one (-> (graph/digraph [1 2] [2 3] [1 4])
                (attr/add-attr 1 :label :one)
                (attr/add-attr [1 2] :label :foo)
                (attr/add-attr [2 3] :label :bar)
                (attr/add-attr [1 4] :label :baz))]
    one))
