(ns fabrikk.directive.interface
  (:require [fabrikk.directive.core :as core]))

(def type-kw core/directive-type-kw)

(defn ->directive
  ([type-kw] (core/->directive type-kw))
  ([type-kw opts] (core/->directive type-kw opts)))

(defn directive? [m]
  (core/directive? m))

(def run core/run)

(defn run-directives [context m]
  (core/run-directives context m))

(defn as [x associate-as]
  (core/as x associate-as))
