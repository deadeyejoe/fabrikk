(ns extrude.build-graph.interface
  (:require [extrude.build-graph.core :as core]))

(defn init
  ([] (core/init))
  ([entity] (core/init entity)))

(defn set-primary! [bg entity]
  (core/set-primary! bg entity))

(defn primary [bg]
  (core/primary bg))

(defn associate [primary label associated-build-graph]
  (core/associate primary label associated-build-graph))

(defn entities-in-build-order [bg]
  (core/entities-in-build-order bg))

(defn path [bg path]
  (core/path bg path))
