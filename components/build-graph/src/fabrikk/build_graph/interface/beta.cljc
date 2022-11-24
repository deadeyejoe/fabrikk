(ns fabrikk.build-graph.interface.beta
  (:require [fabrikk.build-graph.beta.core :as build-graph])
  (:refer-clojure :exclude [merge]))

(defn add-node [build-graph node]
  (build-graph/add-node build-graph node))

(defn add-edge [build-graph source target]
  (build-graph/add-edge build-graph source target))

(defn merge [build-graph other]
  (build-graph/merge build-graph other))

(defn edge-ids-between [build-graph source target]
  (build-graph/edge-ids-between  build-graph source target))

(defn init [& edges]
  (apply build-graph/init edges))

(defn successor-graph [build-graph node]
  (build-graph/successor-graph build-graph node))
