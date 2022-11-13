(ns extrude.execution.interface
  (:require [extrude.execution.core :as core]))

(defn build [factory opts]
  (core/build factory opts))

(defn create [factory opts]
  (core/create factory opts))
