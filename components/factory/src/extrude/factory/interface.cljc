(ns extrude.factory.interface
  (:require [extrude.factory.core :as core]))

(defn ->factory [factory]
  (core/->factory factory))

(defn combine-traits-and-templates [factory opts]
  (core/combine-traits-and-templates factory opts))
