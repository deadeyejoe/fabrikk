(ns fabrikk.factory.interface
  (:require [fabrikk.factory.core :as core]))

(defn ->factory [factory]
  (core/->factory factory))

(defn factory? [x]
  (core/factory? x))

(defn compile-template [factory opts]
  (core/compile-template factory opts))
