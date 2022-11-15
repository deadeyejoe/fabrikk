(ns extrude.factory.interface
  (:require [extrude.factory.core :as core])
  (:refer-clojure :exclude [resolve]))

(defn resolve [id]
  (core/resolve id))

(defn ->factory [factory]
  (core/->factory factory))

(defn factory? [x]
  (core/factory? x))

(defn combine-traits-and-templates [factory opts]
  (core/combine-traits-and-templates factory opts))
