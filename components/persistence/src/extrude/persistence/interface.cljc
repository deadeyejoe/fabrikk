(ns extrude.persistence.interface
  (:require [extrude.persistence.core :as core]))

(defn default-persistence []
  (core/default-persistence-method))

(defn set-default-persistence [method]
  (core/set-default-persistence method))

(def persist! core/persist!)

(def store core/store)

(defn reset-store! []
  (core/reset-store!))
