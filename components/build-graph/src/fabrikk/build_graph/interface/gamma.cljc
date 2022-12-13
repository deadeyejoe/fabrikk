(ns fabrikk.build-graph.interface.gamma
  (:require [fabrikk.build-graph.gamma.core :as value-graph])
  (:refer-clojure :exclude [merge]))

(defn build []
  (value-graph/build))

(defn assoc-node [value-graph id value]
  (value-graph/assoc-node value-graph id value))

(defn add-node [value-graph id value]
  (value-graph/add-node value-graph id value))

(defn set-node [value-graph id value]
  (value-graph/set-node value-graph id value))

(defn update-node [value-graph id f & args]
  (value-graph/update-node value-graph id f args))

(defn node-value [value-graph id]
  (value-graph/node-value value-graph id))

(defn add-edge [value-graph source-id target-id value]
  (value-graph/add-edge value-graph source-id target-id value))

(defn set-edge [value-graph source-id target-id value]
  (value-graph/set-edge value-graph source-id target-id value))

(defn update-edge [value-graph source-id target-id f & args]
  (value-graph/update-edge value-graph source-id target-id f args))

(defn edge-value [value-graph source-id target-id]
  (value-graph/edge-value value-graph source-id target-id))

(defn merge [value-graph other-value-graph & {:keys [combine-node combine-edge]
                                              :or {combine-node (some-fn identity)
                                                   combine-edge (some-fn identity)}}]
  (value-graph/merge value-graph other-value-graph combine-node combine-edge))

(defn successor-graph [value-graph node]
  (value-graph/successor-graph value-graph node))
