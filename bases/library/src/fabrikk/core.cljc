(ns fabrikk.core
  (:require [fabrikk.directives.interface :as directives]
            [fabrikk.execution.interface :as execution]
            [fabrikk.factory.interface :as factory]
            [fabrikk.persistence.interface :as persistence])
  (:refer-clojure :exclude [sequence]))

(defn set-default-persistence [key]
  (persistence/set-default-persistence key))

(defn ->factory [description]
  (factory/->factory description))

(defn build
  ([factory]
   (execution/build factory {}))
  ([factory build-opts]
   (execution/build factory build-opts)))

(defn build-list
  ([factory n]
   (execution/build-list factory n {}))
  ([factory n one+-build-opts]
   (execution/build-list factory n one+-build-opts)))

(defn create
  ([factory]
   (execution/create factory {} {}))
  ([factory build-opts]
   (execution/create factory build-opts {}))
  ([factory build-opts create-opts]
   (execution/create factory build-opts create-opts)))

(defn create-list
  ([factory n]
   (execution/create-list factory n {} {}))
  ([factory n one+-build-opts]
   (execution/create-list factory n one+-build-opts {}))
  ([factory n one+-build-opts create-opts]
   (execution/create-list factory n one+-build-opts create-opts)))

(def persist! persistence/persist!)

(defn constant [x]
  (directives/constant x))

(defn sequence
  ([] (directives/sequence identity nil))
  ([f] (directives/sequence f nil))
  ([f identifier] (directives/sequence f identifier)))

(defn one
  ([factory]
   (directives/build factory {}))
  ([factory build-opts]
   (directives/build factory build-opts)))

(defn many
  ([factory n]
   (directives/build-list factory n {}))
  ([factory n one+-build-opts]
   (directives/build-list factory n one+-build-opts)))
