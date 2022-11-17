(ns fabrikk.factory.interface
  (:require [fabrikk.factory.core :as core])
  (:refer-clojure :exclude [resolve]))

(defn resolve [id]
  (core/resolve id))

(defn ->factory [factory]
  (core/->factory factory))

(defn factory? [x]
  (core/factory? x))

(defn compile-template [factory opts]
  (core/compile-template factory opts))
