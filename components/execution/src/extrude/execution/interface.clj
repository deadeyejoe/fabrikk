(ns extrude.execution.interface
  (:require [extrude.execution.core :as core]))

(defn build-context [factory opts]
  (core/build-context factory opts))

(defn build
  ([factory]
   (core/build factory {}))
  ([factory opts]
   (core/build factory opts)))

(defn build-list-context [factory n opt-list]
  (core/build-list-context factory n opt-list))

(defn build-list
  ([factory n]
   (core/build-list factory n [{}]))
  ([factory n & opt-list]
   (core/build-list factory n opt-list)))

(defn create [factory opts]
  (core/create factory opts))

(defn create-list
  ([factory n]
   (core/create-list factory n [{}]))
  ([factory n & opt-list]
   (core/create-list factory n opt-list)))
