(ns fabrikk.entity.interface
  (:require [fabrikk.entity.core :as core]
            [clojure.spec.alpha :as s]))

(s/def ::instance ::core/instance)
(s/def ::uuid ::core/uuid)

(def id :uuid)

(defn create! [factory build-opts]
  (core/create! factory build-opts))

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

(defn factory [entity]
  (core/factory entity))

(defn factory-id [entity]
  (core/factory-id entity))

(defn associate-as [entity]
  (core/associate-as entity))

(defn value-to-assoc [entity associate-as]
  (core/value-to-assoc entity associate-as))

(def list-item-kw
  core/list-item-kw)

(defn suppress-list-association [entity]
  (core/suppress-list-association entity))

(defn override-association [entity associate-as]
  (core/override-association entity associate-as))

(defn value [entity]
  (core/value entity))

(defn update-value [entity f & args]
  (core/update-value entity f args))

(defn set-persisted-value [entity value]
  (core/set-persisted-value entity value))

(defn persistable? [entity]
  (core/persistable? entity))

(defn persisted? [entity]
  (core/persisted? entity))

(defn pending? [entity]
  (core/pending? entity))

(defn needs-persist? [entity]
  (core/needs-persist? entity))

(defn persist-with [entity]
  (core/persist-with entity))
