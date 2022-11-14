(ns extrude.directive.interface.standard
  (:require [extrude.directive.standard :as standard]))

(defn constant [x]
  (standard/constant x))

(defn build
  ([factory]
   (standard/build factory))
  ([factory opts]
   (standard/build factory opts)))

(defn build-list
  ([factory n]
   (standard/build-list factory n))
  ([factory n & opts]
   (standard/build-list factory n opts)))

(defn provide [build-context]
  (standard/provide build-context))
