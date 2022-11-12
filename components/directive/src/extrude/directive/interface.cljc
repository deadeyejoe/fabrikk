(ns extrude.directive.interface
  (:require [extrude.directive.core :as core]))

(def type-kw core/directive-type-kw)

(defn ->directive
  ([type-kw] (core/->directive type-kw))
  ([type-kw opts] (core/->directive type-kw opts)))

(defn directive? [m]
  (core/directive? m))

(def run core/run)

(defn evaluate [directive]
  (core/evaluate directive))

(defn before [directive f]
  (core/before directive f))

(defn after [directive f]
  (core/after directive f))

(defn run-directives [m]
  (core/run-directives m))

(defn evaluate-directives [m]
  (core/evaluate-directives m))
