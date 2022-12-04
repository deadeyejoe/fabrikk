(ns fabrikk.execution.interface
  (:require [fabrikk.execution.core :as core]))

(defn build-context [factory build-opts]
  (core/build-context factory build-opts))

(defn build [factory build-opts output-opts]
  (core/build factory build-opts output-opts))

(defn build-many [factory n build-opts+]
  (core/build-many factory n build-opts+))

(defn build-list [factory n build-opt-list output-opts]
  (core/build-list factory n build-opt-list output-opts))

(defn create [factory build-opts output-opts]
  (core/create factory build-opts output-opts))

(defn create-list [factory n build-opt-list output-opts]
  (core/create-list factory n build-opt-list output-opts))
