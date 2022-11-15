(ns extrude.build-graph.interface
  (:require [extrude.build-graph.core :as core]))

(defn init
  ([] (core/init))
  ([entity] (core/init entity)))

(defn primary [bg]
  (core/primary bg))

(defn update-primary [bg f & args]
  (tap> [::update bg f args])
  (core/update-primary bg f args))

(defn associate [primary label associated-build-graph]
  (core/associate primary label associated-build-graph))

(defn entities-in-build-order [bg]
  (core/entities-in-build-order bg))

(defn path [bg path]
  (core/path bg path))
