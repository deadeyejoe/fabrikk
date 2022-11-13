(ns extrude.execution-context.interface
  (:require [extrude.build-graph.interface :as build-graph]))

(defn init []
  (build-graph/init))

(defn set-primary! [context entity]
  (build-graph/set-primary! context entity))

(defn entity->context [entity]
  (build-graph/entity->build-graph entity))

(defn associate [primary label associated-context]
  (build-graph/associate primary label associated-context))
