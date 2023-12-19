(ns fabrikk.execution.interface
  (:require [fabrikk.execution.core :as core]))

( defn compile-template [factory build-opts]
 (core/compile-template factory build-opts))

(defn build-context [factory build-opts]
  (core/build-context factory build-opts))

(defn build-many [factory n build-opts+]
  (core/build-many factory n build-opts+))

(defn build-list-context [factory number build-opt+]
  (core/build-list-context factory number build-opt+))

(defn persist-context [output-opts built-context]
  (core/persist-context output-opts built-context))
