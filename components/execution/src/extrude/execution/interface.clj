(ns extrude.execution.interface
  (:require [extrude.execution.core :as core]))

(defn build-context [factory opts]
  (core/build-context factory opts))

(defn build
  ([factory]
   (core/build factory {}))
  ([factory opts]
   (core/build factory opts)))

(defn create [factory opts]
  (core/create factory opts))
