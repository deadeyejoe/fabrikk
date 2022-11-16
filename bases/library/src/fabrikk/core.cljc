(ns fabrikk.core
  (:require [fabrikk.directives.interface :as directives]
            [fabrikk.execution.interface :as execution]
            [fabrikk.factory.interface :as factory])
  (:refer-clojure :exclude [sequence]))

(defn ->factory [description]
  (factory/->factory description))

(defn build
  ([factory]
   (execution/build factory {}))
  ([factory opts]
   (execution/build factory opts)))

(defn build-list
  ([factory n]
   (execution/build-list factory n [{}]))
  ([factory n & opt-list]
   (execution/build-list factory n opt-list)))

(defn create
  ([factory]
   (execution/create factory {}))
  ([factory opts]
   (execution/create factory opts)))

(defn create-list
  ([factory n]
   (execution/create-list factory n [{}]))
  ([factory n & opt-list]
   (execution/create-list factory n opt-list)))

(defn constant [x]
  (directives/constant x))

(defn sequence
  ([] (directives/sequence identity nil))
  ([f] (directives/sequence f nil))
  ([f identifier] (directives/sequence f identifier)))

(defn one
  ([factory]
   (directives/build factory))
  ([factory opts]
   (directives/build factory opts)))

(defn many
  ([factory n]
   (directives/build-list factory n))
  ([factory n & opts]
   (directives/build-list factory n opts)))
