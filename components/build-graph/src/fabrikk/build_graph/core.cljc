(ns fabrikk.build-graph.core
  (:require [clojure.spec.alpha :as s]
            [fabrikk.build-graph.label-graph :as label-graph]
            [fabrikk.entity.interface :as entity]
            [loom.graph :as graph]
            [loom.alg :as graph-alg]))

(s/def ::primary ::entity/instance)
(s/def ::codex (s/map-of ::entity/uuid ::entity/instance))
(s/def ::instance (s/keys :opt-un [::graph
                                   ::primary
                                   ::codex]))

(defrecord BuildGraph [codex labels]
  graph/Graph
  (edges [_]
    (mapcat (fn [[src label->dest]]
              (map (partial vector src) (vals label->dest)))
            labels))
  (has-edge? [_ src dest]
    (->> (get labels src)
         (vals)
         (some (partial = dest))))
  (nodes [_]
    (keys codex))
  (has-node? [_ node]
    (get codex node))
  (out-degree [_ src-node]
    (count (get labels src-node)))
  (out-edges [_ src-node]
    (->> (get labels src-node)
         (vals)
         (map (partial vector src-node))))
  (successors* [x src-node]
    (map second (graph/out-edges x src-node)))
  graph/Digraph
  (in-edges [_ dest-node]
    (map (juxt first last)
         (label-graph/in-edges labels dest-node)))
  (in-degree [x dest-node]
    (count (graph/in-edges x dest-node)))
  (predecessors* [x dest-node]
    (map first (graph/out-edges x dest-node)))
  (transpose [_]
    (throw (Exception. "Not implemented"))))

(defn in-edges [{:keys [labels] :as bg} node]
  (label-graph/in-edges labels node))

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

(defn assert-node [{:keys [codex] :as bg} {:keys [uuid] :as entity}]
  (assert (get codex uuid) "Entity was not found in the graph!"))

(defn set-primary! [bg {:keys [uuid] :as entity}]
  (-> bg
      (ensure-node entity)
      (assoc :primary uuid)))

(defn entity [bg id]
  (get-in bg [:codex id]))

(defn update-entity [bg id f args]
  (apply update-in bg [:codex id] f args))

(defn primary [{:keys [primary] :as bg}]
  (entity bg primary))

(defn update-primary [{:keys [primary] :as bg} f args]
  (update-entity bg primary f args))

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

(defn add-link [{:keys [primary] :as build-graph}
                label
                entity]
  (assert-node build-graph entity)
  (link build-graph primary label entity))

(defn path
  "Given a build graph and a path comprised of a sequence of labels. Starting at the 
   primary node, traverse edges with each label in turn, and return the node at the end of
   the path, if it exists. Or nil otherwise"
  [{:keys [codex labels primary] :as _build-graph} path]
  (get codex (label-graph/traverse-path labels primary path)))

(defn entities-in-build-order [{:keys [codex] :as build-graph}]
  (if-let [sorted (graph-alg/topsort build-graph)]
    (reverse sorted)
    (throw (IllegalArgumentException. "Build graph must be a DAG"))))

(comment
  (-> (->BuildGraph {1 :one 2 :two}
                    {1 {:org 2}})
      (graph-alg/topsort)))
