(ns extrude.entity.interface
  (:require [extrude.entity.core :as core]))

(defn build [factory value]
  (core/build factory value))

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
