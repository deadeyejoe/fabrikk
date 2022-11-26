(ns fabrikk.build-context.interface
  (:require [fabrikk.build-context.core :as bc])
  (:refer-clojure :exclude [merge]))

(defn init [primary]
  (bc/init primary))

(defn entity [build-context id]
  (bc/entity build-context id))

(defn update-entity [build-context id f & args]
  (bc/update-entity build-context id f args))

(defn primary [build-context]
  (bc/primary build-context))

(defn update-primary [build-context f & args]
  (bc/update-primary build-context f args))

(defn associate-entity
  ([build-context label target-entity]
   (bc/associate-entity build-context label target-entity))
  ([build-context source-entity label target-entity]
   (bc/associate-entity  build-context source-entity label target-entity)))

(defn associate-context
  ([build-context label target-context]
   (bc/associate-context build-context label target-context))
  ([build-context source-entity label target-context]
   (bc/associate-context build-context source-entity label target-context)))

(defn propagate [build-context entity]
  (bc/propagate build-context entity))

(defn path [build-context path]
  (bc/path build-context path))
