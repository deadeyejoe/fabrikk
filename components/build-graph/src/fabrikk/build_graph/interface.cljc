(ns fabrikk.build-graph.interface
  (:require [fabrikk.build-graph.core :as core]))

(defn init
  ([] (core/init))
  ([entity] (core/init entity)))

(defn primary [bg]
  (core/primary bg))

(defn update-primary [bg f & args]
  (core/update-primary bg f args))

(defn update-primary-value [bg f & args]
  (core/update-primary-value bg f args))

(defn entity [bg id]
  (core/entity bg id))

(defn set-entity [bg entity]
  (core/ensure-node bg entity))

(defn update-entity [bg id f & args]
  (core/update-entity bg id f args))

(defn update-entity-value [bg id f & args]
  (core/update-entity-value bg id f args))

(defn associate [build-graph link associated-build-graph]
  (core/associate build-graph link associated-build-graph))

(defn add-link [build-graph link entity]
  (core/add-link build-graph link entity))

(defn entities-in-build-order [bg]
  (core/entities-in-build-order bg))

(defn in-edges-with-links [bg node]
  (core/in-edges-with-links bg node))

(defn path [bg path]
  (core/path bg path))
