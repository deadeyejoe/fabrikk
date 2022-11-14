(ns extrude.build-graph.interface
  (:require [extrude.build-graph.core :as core]))

(defn init []
  (core/init))

(defn set-primary! [bg entity]
  (core/set-primary! bg entity))

(defn entity->build-graph [entity]
  (core/entity->build-graph entity))

(defn associate [primary label associated-build-graph]
  (core/associate primary label associated-build-graph))

(defn path [bg path]
  (core/path bg path))