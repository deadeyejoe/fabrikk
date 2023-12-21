(ns fabrikk.alpha.core
  (:require [fabrikk.build-context.interface :as context]
            [fabrikk.directives.interface :as directives]
            [fabrikk.directive-core.interface :as directive-core]
            [fabrikk.entity.interface :as entity]
            [fabrikk.factory.interface :as factory]
            [fabrikk.persistence.interface :as persistence])
  (:refer-clojure :exclude [sequence derive]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Define factories
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->factory [description]
  (factory/->factory description))

(defn inherit [factory-or-kw description]
  (factory/inherit factory-or-kw description))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Build & Create
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build
  ([factory]
   (factory/build factory {} {}))
  ([factory build-opts]
   (factory/build factory build-opts {}))
  ([factory build-opts output-opts]
   (factory/build factory build-opts output-opts)))

(defn build-list
  ([factory n]
   (factory/build-list factory n {} {}))
  ([factory n one+-build-opts]
   (factory/build-list factory n one+-build-opts {}))
  ([factory n one+-build-opts output-opts]
   (factory/build-list factory n one+-build-opts output-opts)))

(defn create
  ([factory]
   (factory/create factory {} {}))
  ([factory build-opts]
   (factory/create factory build-opts {}))
  ([factory build-opts output-opts]
   (factory/create factory build-opts output-opts)))

(defn create-list
  ([factory n]
   (factory/create-list factory n {} {}))
  ([factory n one+-build-opts]
   (factory/create-list factory n one+-build-opts {}))
  ([factory n one+-build-opts output-opts]
   (factory/create-list factory n one+-build-opts output-opts)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Directives
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
   (directives/derive key-or-path nil))
  ([key-or-path f]
   (directives/derive key-or-path f)))

(defn associate-as [entity associate-as]
  (directive-core/associate-as entity associate-as))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Context 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn path [context path]
  (entity/value (context/path context path)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Persistence
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def store persistence/store)

(defn reset-store! []
  (persistence/reset-store!))

(defn set-default-persistence [key]
  (persistence/set-default-persistence key))

(def persist! persistence/persist!)
