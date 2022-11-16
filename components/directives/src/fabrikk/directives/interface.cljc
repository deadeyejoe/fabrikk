(ns fabrikk.directives.interface
  (:require [fabrikk.directives.core :as core])
  (:refer-clojure :exclude [sequence]))

(defn constant [x]
  (core/constant x))

(defn sequence
  ([] (core/sequence identity nil))
  ([f] (core/sequence f nil))
  ([f identifier] (core/sequence f identifier)))

(defn build
  ([factory]
   (core/build factory))
  ([factory opts]
   (core/build factory opts)))

(defn build-list
  ([factory n]
   (core/build-list factory n))
  ([factory n & opts]
   (core/build-list factory n opts)))
