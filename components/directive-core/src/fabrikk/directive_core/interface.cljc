(ns fabrikk.directive-core.interface
  (:require [clojure.spec.alpha :as s]
            [fabrikk.directive-core.core :as core]))

(s/def ::directive ::core/directive)

(def type-kw core/directive-type-kw)

(defn ->directive
  ([type-kw] (core/->directive type-kw))
  ([type-kw opts] (core/->directive type-kw opts)))

(defn directive? [m]
  (core/directive? m))

(def run core/run)

(defn run-directives [context m]
  (core/run-directives context m))
