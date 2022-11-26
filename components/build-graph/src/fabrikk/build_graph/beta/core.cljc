(ns fabrikk.build-graph.beta.core
  "A directed graph where each edge has a unique id"
  (:require [clojure.set :as set]
            [loom.graph :as graph]
            [medley.core :as medley]
            [loom.alg :as graph-alg])
  (:refer-clojure :exclude [merge]))

(def collect-set (fnil conj #{}))

(defn add-node [{:keys [nodeset] :as build-graph} node]
  (if (contains? nodeset node)
    build-graph
    (update build-graph :nodeset collect-set node)))

(defn import-edge [build-graph edge-id source target]
  (-> build-graph
      (add-node source)
      (add-node target)
      (update-in [:source->edgeset source] collect-set edge-id)
      (update-in [:target->edgeset target] collect-set edge-id)
      (assoc-in [:id->edge edge-id] [source target])))

(defn add-edge [build-graph source target]
  (let [edge-id (random-uuid)]
    [(import-edge build-graph edge-id source target) edge-id]))

(defn merge [build-graph other]
  (-> build-graph
      (update :nodeset set/union (:nodeset other))
      (update :source->edgeset (partial merge-with set/union) (:source->edgeset other))
      (update :target->edgeset (partial merge-with set/union) (:target->edgeset other))
      (update :id->edge clojure.core/merge (:id->edge other))))

(defn edge-ids [{:keys [id->edge] :as _build-graph}]
  (keys id->edge))

(defn id->edge [build-graph id]
  (get-in build-graph [:id->edge id]))

(defn edge-ids-between [{:keys [source->edgeset target->edgeset]
                         :as _build-graph} source target]
  (set/intersection (get source->edgeset source)
                    (get target->edgeset target)))

(defn edges-between [{:keys [id->edge] :as build-graph} source target]
  (->> (edge-ids-between build-graph source target)
       (map id->edge)))

(defrecord BuildGraph [nodeset source->edgeset target->edgeset id->edge]
  graph/Graph
  (edges [_]
    (vals id->edge))
  (has-edge? [build-graph source target]
    (not-empty (edge-ids-between build-graph source target)))
  (nodes [_] nodeset)
  (has-node? [_ node]
    (or (contains? source->edgeset node)
        (contains? target->edgeset node)))
  (out-degree [_ source]
    (count (get source->edgeset source)))
  (out-edges [_ source]
    (->> (get source->edgeset source)
         (map id->edge)))
  (successors* [bg source]
    (map second (graph/out-edges bg source)))

  graph/Digraph
  (in-edges [_ target]
    (->> (get target->edgeset target)
         (map id->edge)))
  (in-degree [_ target]
    (count (get target->edgeset target)))
  (predecessors* [bg dest-node]
    (map first (graph/in-edges bg dest-node)))
  (transpose [_]
    (map->BuildGraph {:source->edgeset target->edgeset
                      :target->edgeset source->edgeset
                      :id->edge (medley/map-vals (fn [[source target]]
                                                   [target source])
                                                 id->edge)})))

(defn init [& edges]
  (reduce (fn [graph [source target :as _edge]]
            (first (add-edge graph source target)))
          (map->BuildGraph {:source->edgeset {}
                            :target->edgeset {}
                            :id->edge {}})
          edges))

(defn successor-graph [{:keys [id->edge] :as build-graph} node]
  (let [successor-set (set (graph-alg/post-traverse build-graph node))]
    (reduce-kv (fn [new-graph edge-id [source target]]
                 (if (and (contains? successor-set source)
                          (contains? successor-set target))
                   (import-edge new-graph edge-id source target)
                   new-graph))
               (init)
               id->edge)))
