(ns extrude.build-graph.interface
  (:require [extrude.build-graph.core :as core]))

(defn init
  ([] (core/init))
  ([entity] (core/init entity)))

(defn primary [bg]
  (core/primary bg))

(defn update-primary [bg f & args]
  (core/update-primary bg f args))

(defn entity [bg id]
  (core/entity bg id))

(defn set-entity [bg entity]
  (core/ensure-node bg entity))

(defn update-entity [bg id f & args]
  (core/update-entity bg id f args))

(defn associate [primary label associated-build-graph]
  (core/associate primary label associated-build-graph))

(defn entities-in-build-order [bg]
  (core/entities-in-build-order bg))

(defn in-edges-with-labels [bg node]
  (core/in-edges bg node))

(defn path [bg path]
  (core/path bg path))
