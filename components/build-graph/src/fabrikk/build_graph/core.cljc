(ns fabrikk.build-graph.core
  (:require [loom.graph :as graph]
            [medley.core :as medley]
            [clojure.set :as set])
  (:refer-clojure :exclude [merge]))

(def collect-set (fnil conj #{}))

(defn reverse-adjacency [node->nodeset]
  (->> node->nodeset
       (mapcat (fn [[node nodeset]] (map #(vector % node) nodeset)))
       (reduce (fn [acc [from to]]
                 (update acc from collect-set to))
               {})))

(defrecord ValueGraph [node-id->value edge-id->value source->target-set target->source-set]
  graph/Graph
  (edges [_]
    (keys edge-id->value))
  (has-edge? [_ source target]
    (contains? (get source->target-set source) target))
  (nodes [_]
    (keys node-id->value))
  (has-node? [_ node]
    (contains? node-id->value node))
  (out-degree [_ source]
    (count (get source->target-set source)))
  (out-edges [_ source]
    (->> (get source->target-set source)
         (map (partial vector source))))
  (successors* [_ source]
    (get source->target-set source))

  graph/Digraph
  (in-edges [_ target]
    (->> (get target->source-set target)
         (map #(vector % target))))
  (in-degree [_ target]
    (count (get target->source-set target)))
  (predecessors* [_ target]
    (get target->source-set target))
  (transpose [value-graph]
    (-> value-graph
        (update :edge-id->value (partial medley/map-keys reverse))
        (update :source->target-set reverse-adjacency)
        (update :target->source-set reverse-adjacency))))

(defn build []
  (->ValueGraph {} {} {} {}))

(defn insert-node [value-graph id value]
  (assoc-in value-graph [:node-id->value id] value))

(defn add-node
  "Add a node if it's not already there"
  [value-graph id value]
  (assert (not (graph/has-node? value-graph id)))
  (insert-node value-graph id value))

(defn set-node
  "Set the value of an existing node"
  [value-graph id value]
  (assert (graph/has-node? value-graph id))
  (insert-node value-graph id value))

(defn update-node
  "Update the value of an existing node"
  [value-graph id f args]
  (assert (graph/has-node? value-graph id))
  (apply update-in value-graph [:node-id->value id] f args))

(defn node-value [value-graph id]
  (get-in value-graph [:node-id->value id]))

(defn collect-edge [value-graph source-id target-id]
  (-> value-graph
      (update-in [:source->target-set source-id] collect-set target-id)
      (update-in [:target->source-set target-id] collect-set source-id)))

(defn add-edge [value-graph source-id target-id value]
  (assert (and (graph/has-node? value-graph source-id)
               (graph/has-node? value-graph target-id)
               (not (graph/has-edge? value-graph source-id target-id))))
  (-> value-graph
      (collect-edge source-id target-id)
      (assoc-in [:edge-id->value [source-id target-id]] value)))

(defn set-edge [value-graph source-id target-id value]
  (assert (graph/has-edge? value-graph source-id target-id))
  (assoc-in value-graph [:edge-id->value [source-id target-id]] value))

(defn update-edge [value-graph source-id target-id f args]
  (-> value-graph
      (collect-edge source-id target-id)
      (update-in [:edge-id->value [source-id target-id]] #(apply f % args))))

(defn edge-value [value-graph source-id target-id]
  (get-in value-graph [:edge-id->value [source-id target-id]]))

(defn merge [value-graph other-graph combine-node combine-edge]
  (-> value-graph
      (update :node-id->value #(merge-with combine-node % (:node-id->value other-graph)))
      (update :edge-id->value #(merge-with combine-edge % (:edge-id->value other-graph)))
      (update :source->target-set #(merge-with set/union % (:source->target-set other-graph)))
      (update :target->source-set #(merge-with set/union % (:target->source-set other-graph)))))

(defn successor-edges [value-graph node]
  (loop [edges #{}
         visited #{}
         [current-node & rest :as nodes] [node]]
    (cond
      (empty? nodes) edges
      (contains? visited current-node) (recur edges visited rest)
      :else (let [outward (set (graph/out-edges value-graph current-node))]
              (recur (apply conj edges outward)
                     (conj visited current-node)
                     (apply conj rest (map second outward)))))))

(defn successor-graph [value-graph node]
  (when (graph/has-node? value-graph node)
    (let [edges (successor-edges value-graph node)
          nodes (-> edges vec flatten set (conj node))
          new-graph-with-edges (reduce (partial apply collect-edge)
                                       (build)
                                       edges)]
      (-> new-graph-with-edges
          (assoc :node-id->value (select-keys (:node-id->value value-graph) nodes))
          (assoc :edge-id->value (select-keys (:edge-id->value value-graph) edges))))))
