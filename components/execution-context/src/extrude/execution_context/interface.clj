(ns extrude.execution-context.interface
  (:require [extrude.build-graph.interface :as build-graph]
            [extrude.execution-context.core :as core]))

(defn init []
  (build-graph/init))

(defn set-primary! [context entity]
  (build-graph/set-primary! context entity))

(defn entity->context [entity]
  (build-graph/entity->build-graph entity))

(defn associate [context label associated-context]
  (build-graph/associate context label associated-context))

(defn assoc-value [context key value]
  (core/assoc-value context key value))

(defn ->result-meta [context]
  (core/->result-meta context))
