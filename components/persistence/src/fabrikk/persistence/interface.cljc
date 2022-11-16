(ns fabrikk.persistence.interface
  (:require [fabrikk.persistence.core :as core]))

(defn default-persistence []
  (core/default-persistence-method))

(defn set-default-persistence [method]
  (core/set-default-persistence method))

(def persist! core/persist!)

(def store core/store)

(defn store! [entity]
  (core/store! entity))

(defn reset-store! []
  (core/reset-store!))
