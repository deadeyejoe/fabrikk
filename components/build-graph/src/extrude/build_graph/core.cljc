(ns extrude.build-graph.core
  (:require [extrude.entity.interface :as entity]
            [extrude.build-graph.labels :as labels]
            [clojure.spec.alpha :as s]
            [loom.graph :as graph]))

(s/def ::graph graph/directed?)
(s/def ::primary ::entity/instance)
(s/def ::codex (s/map-of ::entity/uuid ::entity/instance))
(s/def ::instance (s/keys :opt-un [::graph
                                   ::primary
                                   ::codex]))

(defn init []
  {:graph (graph/digraph)
   :labels {}
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
        (update :labels labels/collect edge label))))

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

(defn merge-builds [{:keys [graph labels codex] :as primary} to-merge]
  (-> primary
      (assoc :codex (merge-with entity/combine-no-conflict codex (:codex to-merge)))
      (assoc :labels (labels/merge-attrs labels (:labels to-merge)))
      (assoc :graph (merge-graphs graph (:graph to-merge)))))

(defn associate [primary label {associated-primary :primary :as associated-build-graph}]
  (-> (entity->build-graph primary)
      (merge-builds associated-build-graph)
      (link (:uuid primary) label associated-primary)))

(defn path
  "Given a build graph and a path comprised of a sequence of labels. Starting at the 
   primary node, traverse edges with each label in turn, and return the node at the end of
   the path, if it exists. Or nil otherwise"
  [{:keys [codex labels primary]} path]
  (when-let [traversed-edges (labels/traverse-path labels primary path)]
    (get codex (-> traversed-edges last last))))
