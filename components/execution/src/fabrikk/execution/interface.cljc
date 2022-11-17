(ns fabrikk.execution.interface
  (:require [fabrikk.execution.core :as core]))

(defn build-context [factory build-opts]
  (core/build-context factory build-opts))

(defn build [factory build-opts]
  (core/build factory build-opts))

(defn build-list-context [factory n build-opt-list]
  (core/build-list-context factory n build-opt-list))

(defn build-list [factory n build-opt-list]
  (core/build-list factory n build-opt-list))

(defn create [factory build-opts create-opts]
  (core/create factory build-opts create-opts))

(defn create-list [factory n build-opt-list create-opts]
  (core/create-list factory n build-opt-list create-opts))
