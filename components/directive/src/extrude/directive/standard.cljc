(ns extrude.directive.standard
  (:require [extrude.directive.core :as core]
            [extrude.entity.interface :as entity]
            [extrude.execution.interface :as execution]
            [extrude.execution-context.interface :as context]
            [clojure.spec.alpha :as s]))

(s/def ::directive map?)

;; =========== CONSTANT ===========

(defn constant [x]
  (core/->directive ::constant
                    {:value x}))
(s/fdef constant
  :args (s/cat :x any?)
  :ret ::directive)

(defmethod core/run ::constant [context key {:keys [value] :as _directive}]
  value)

;; =========== BUILD ===========

(defn build
  ([factory] (build factory {}))
  ([factory build-opts]
   (core/->directive ::build
                     {:value factory
                      :build-opts build-opts})))

(defn link-to-context [context key built-context]
  (-> context
      (context/associate key built-context)
      (context/assoc-value key (context/->result-meta built-context))))

(defmethod core/run ::build [context key {:keys [value build-opts] :as _directive}]
  (link-to-context context 
                   key 
                   (execution/build-context value build-opts)))

;; =========== BUILD LIST ===========

(defn build-list
  ([factory n] (build-list factory n [{}]))
  ([factory n build-opt-list]
   (core/->directive ::build-list
                     {:value factory
                      :number n
                      :build-opt-list build-opt-list})))

(defmethod core/run ::build-list [context key
                                  {:keys [value number build-opt-list]
                                   :or {number 0}
                                   :as _directive}]
  (link-to-context context 
                   key
                   (execution/build-list-context value number build-opt-list)))

;; =========== PROVIDE ===========

(defn provide [build-context]
  (core/->directive ::provide
                    {:value build-context}))

(defmethod core/run ::provide [context key {provided :value}]
  (if-let [provided-context (meta provided)]
    (link-to-context context key provided-context)
    (context/assoc-value context key provided)))
