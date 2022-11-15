(ns extrude.build-graph.core
  (:require [clojure.spec.alpha :as s]
            [extrude.build-graph.label-graph :as label-graph]
            [extrude.entity.interface :as entity]
            [loom.graph :as graph]
            [loom.alg :as graph-alg]))

(s/def ::primary ::entity/instance)
(s/def ::codex (s/map-of ::entity/uuid ::entity/instance))
(s/def ::instance (s/keys :opt-un [::graph
                                   ::primary
                                   ::codex]))

(defrecord BuildGraph [codex labels]
  graph/Graph
  (edges [x]
    (mapcat (fn [[src label->dest]]
              (map (partial vector src) (vals label->dest)))
            labels))
  (has-edge? [x src dest]
    (->> (get labels src)
         (vals)
         (some (partial = dest))))
  (nodes [x]
    (keys codex))
  (has-node? [x node]
    (get codex node))
  (out-degree [x src-node]
    (count (get labels src-node)))
  (out-edges [x src-node]
    (->> (get labels src-node)
         (vals)
         (map (partial vector src-node))))
  (successors* [x src-node]
    (map second (graph/out-edges x src-node)))
  graph/Digraph
  (in-edges [x dest-node]
    (keep (fn [[src label->dest]]
            (when (some #{dest-node}
                        (vals label->dest))
              [src dest-node]))
          labels))
  (in-degree [x dest-node]
    (count (graph/in-edges x dest-node)))
  (predecessors* [x dest-node]
    (map first (graph/out-edges x dest-node)))
  (transpose [x]
    (throw (Exception. "Not implemented"))))

(declare set-primary!)

(defn init
  ([]
   (assoc (->BuildGraph {} {})
          :primary nil))
  ([entity] (-> (init) 
                (set-primary! entity))))

(defn ensure-node [{:keys [codex] :as bg} {:keys [uuid] :as entity}]
  (if-let [existing (get codex uuid)]
    (assoc-in bg [:codex uuid] (entity/combine-no-conflict existing entity))
    (assoc-in bg [:codex uuid] entity)))

(defn set-primary! [bg {:keys [uuid] :as entity}]
  (-> bg
      (ensure-node entity)
      (assoc :primary uuid)))

(defn primary [{:keys [primary] :as bg}]
  (get-in bg [:codex primary]))

(defn update-primary [{:keys [primary] :as bg} f args]
  (apply update-in bg [:codex primary] f args))

(defn link [bg entity-id label other-entity-id] 
  (update bg :labels label-graph/link entity-id label other-entity-id))

(defn merge-builds [{:keys [labels codex] :as primary} to-merge]
  (-> primary
      (assoc :codex (merge-with entity/combine-no-conflict codex (:codex to-merge)))
      (assoc :labels (label-graph/merge labels (:labels to-merge)))))

(defn associate [{:keys [primary] :as build-graph} 
                 label
                 {associated-primary :primary :as associated-build-graph}]
  (-> build-graph
      (merge-builds associated-build-graph)
      (link primary label associated-primary)))

(defn path
  "Given a build graph and a path comprised of a sequence of labels. Starting at the 
   primary node, traverse edges with each label in turn, and return the node at the end of
   the path, if it exists. Or nil otherwise"
  [{:keys [codex labels primary] :as _build-graph} path]
  (get codex (label-graph/traverse-path labels primary path)))

(defn entities-in-build-order [{:keys [codex] :as build-graph}]
  (if-let [sorted (graph-alg/topsort build-graph)]
    (->> sorted
         (reverse)
         (map codex))
    (throw (IllegalArgumentException. "Build graph must be a DAG"))))
