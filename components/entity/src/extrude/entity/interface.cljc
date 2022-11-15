(ns extrude.entity.interface
  (:require [extrude.entity.core :as core]
            [clojure.spec.alpha :as s]))

(s/def ::instance ::core/instance)
(s/def ::uuid ::core/uuid)

(defn create! [factory value]
  (core/create! factory value))

(defn create-list! []
  (core/create-list!))

(defn refresh-uuid [entity]
  (core/refresh-uuid entity))

(defn conflict? [entity other-entity]
  (core/conflict? entity other-entity))

(defn no-conflict? [entity other-entity]
  (core/no-conflict? entity other-entity))

(defn assert-no-conflict [entity other-entity]
  (core/assert-no-conflict entity other-entity))

(defn combine [entity other-entity]
  (core/combine entity other-entity))

(defn combine-no-conflict [entity other-entity]
  (core/combine-no-conflict entity other-entity))

(defn factory-id [entity]
  (core/factory-id entity))

(defn value [entity]
  (core/value entity))

(defn update-value [entity f & args]
  (core/update-value entity f args))

(defn needs-persist? [entity]
  (core/needs-persist? entity))
