(ns extrude.execution-context.interface
  (:require [extrude.build-graph.interface :as build-graph]
            [extrude.execution-context.core :as core]))

(defn init
  ([] (build-graph/init))
  ([entity] (build-graph/init entity)))

(defn primary [context]
  (build-graph/primary context))

(defn update-primary [context f & args]
  (apply build-graph/update-primary context f args))

(defn entity [context id]
  (build-graph/entity context id))

(defn set-entity [context entity]
  (build-graph/set-entity context entity))

(defn update-entity [context id f & args]
  (apply build-graph/update-entity context id f args))

(defn associate [context label associated-context]
  (core/associate context label associated-context))

(defn entities-in-build-order [context]
  (build-graph/entities-in-build-order context))

(defn propagate [context entity]
  (core/propagate context entity))

(defn path [context path]
  (build-graph/path context path))

(defn assoc-value [context key value]
  (core/assoc-value context key value))

(defn update-value [context f & args]
  (core/update-value context f args))

(defn ->result [context]
  (core/->result context))

(defn ->result-meta [context]
  (core/->result-meta context))

(defn meta-result? [x]
  (core/meta-result? x))
