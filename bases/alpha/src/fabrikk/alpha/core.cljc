(ns fabrikk.alpha.core
  (:require [fabrikk.directives.interface :as directives]
            [fabrikk.entity.interface :as entity]
            [fabrikk.build-context.interface :as context]
            [fabrikk.execution.interface :as execution]
            [fabrikk.factory.interface :as factory]
            [fabrikk.persistence.interface :as persistence]
            [fabrikk.template.interface :as template])
  (:refer-clojure :exclude [sequence derive]))

;; ============== Define factories

(defn ->factory [description]
  (factory/->factory description))

;; =============== Templates

(defn update-template [template field value]
  (template/update template field value))

;; =============== Context 

(defn path [context path]
  (entity/value (context/path context path)))

;; ================ Persistence

(def store persistence/store)

(defn reset-store! []
  (persistence/reset-store!))

(defn set-default-persistence [key]
  (persistence/set-default-persistence key))

(def persist! persistence/persist!)

;; =============== Build & Create

(defn build
  ([factory]
   (execution/build factory {} {}))
  ([factory build-opts]
   (execution/build factory build-opts {}))
  ([factory build-opts output-opts]
   (execution/build factory build-opts output-opts)))

(defn build-list
  ([factory n]
   (execution/build-list factory n {} {}))
  ([factory n one+-build-opts]
   (execution/build-list factory n one+-build-opts {}))
  ([factory n one+-build-opts output-opts]
   (execution/build-list factory n one+-build-opts output-opts)))

(defn create
  ([factory]
   (execution/create factory {} {}))
  ([factory build-opts]
   (execution/create factory build-opts {}))
  ([factory build-opts output-opts]
   (execution/create factory build-opts output-opts)))

(defn create-list
  ([factory n]
   (execution/create-list factory n {} {}))
  ([factory n one+-build-opts]
   (execution/create-list factory n one+-build-opts {}))
  ([factory n one+-build-opts output-opts]
   (execution/create-list factory n one+-build-opts output-opts)))

;; ============= Directives

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

(defn derive
  ([key-or-path]
   (directives/derive key-or-path identity))
  ([key-or-path f]
   (directives/derive key-or-path f)))
