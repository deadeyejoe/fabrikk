(ns extrude.execution-context.interface
  (:require [extrude.build-graph.interface :as build-graph]
            [extrude.execution-context.core :as core]))

(defn init
  ([] (build-graph/init))
  ([entity] (build-graph/init entity)))

(defn set-primary! [context entity]
  (build-graph/set-primary! context entity))

(defn primary [context]
  (build-graph/primary context))

(defn entity->context [entity]
  (build-graph/entity->build-graph entity))

(defn ->list-context []
  (core/->list-context))

(defn associate [context label associated-context]
  (build-graph/associate context label associated-context))

(defn entities-in-build-order [context]
  (build-graph/entities-in-build-order context))

(defn path [context path]
  (build-graph/path context path))

(defn assoc-value [context key value]
  (core/assoc-value context key value))

(defn update-value [context f & args]
  (core/update-value context f args))

(defn ->result-meta [context]
  (core/->result-meta context))

(defn meta-result? [x]
  (core/meta-result? x))
