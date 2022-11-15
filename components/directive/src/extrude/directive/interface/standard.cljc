(ns extrude.directive.interface.standard
  (:require [extrude.directive.standard :as standard])
  (:refer-clojure :exclude [sequence]))

(defn constant [x]
  (standard/constant x))
 
(defn sequence
  ([] (standard/sequence identity nil))
  ([f] (standard/sequence f nil))
  ([f identifier] (standard/sequence f identifier)))

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
