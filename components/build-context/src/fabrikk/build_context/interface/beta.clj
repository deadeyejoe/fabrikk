(ns fabrikk.build-context.interface.beta
  (:require [fabrikk.build-context.beta.core :as build-context]))

(defn init [primary]
  (build-context/init primary))

(defn id->entity [build-context id]
  (build-context/id->entity build-context id))

(defn primary [build-context]
  (build-context/primary build-context))

(defn update-entity [build-context id f & args]
  (build-context/update-entity build-context id f args))

(defn update-primary [build-context f args]
  (build-context/update-primary build-context f args))

(defn associate-entity
  ([build-context label target-entity]
   (build-context/associate-entity build-context label target-entity))
  ([build-context source-entity label target-entity]
   (build-context/associate-entity build-context source-entity label target-entity)))

(defn associate-context
  ([build-context label target-context]
   (build-context/associate-context build-context label target-context))
  ([build-context source-entity label target-context]
   (build-context/associate-context build-context source-entity label target-context)))

(defn propagate [build-context pending-entity]
  (build-context/propagate build-context pending-entity))

(defn traverse-path [build-context path]
  (build-context/traverse-path build-context path))

(defn path [build-context path]
  (build-context/path build-context path))
