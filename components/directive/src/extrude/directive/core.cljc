(ns extrude.directive.core
  (:require [clojure.spec.alpha :as s]
            [extrude.entity.interface :as entity]
            [extrude.execution-context.interface :as context]))

(s/def ::type qualified-keyword?)
(s/def ::value any?)
(s/def ::directive (s/keys :req [::type]
                           :opt-un [::value]))


(def directive-type-kw ::type)

(defn ->directive
  ([type-kw] (->directive type-kw {}))
  ([type-kw opts]
   (merge {directive-type-kw type-kw} opts)))
(s/fdef ->directive
  :args (s/cat :type-kw qualified-keyword? :opts (s/? map?))
  :ret ::directive)

(defn run-directive-dispatch [_ _ directive]
  (get directive directive-type-kw))
(defmulti run #'run-directive-dispatch)
(defmethod run :default [build-context key value]
  (if (context/meta-result? value)
    (context/associate build-context key (meta value))
    (context/assoc-value build-context key (if (fn? value)
                                             (value)
                                             value))))

(defprotocol Directive
  (evaluate [this build-context key]))

(defn directive? [m]
  (or (contains? m directive-type-kw)
      (satisfies? m Directive)))

(comment
  (defrecord Constant [value]
    Directive
    (evaluate [x build-context key]
      (context/assoc-value build-context key value)))
  (evaluate (->Constant 1) {} :foo))

;; =========== UTILS ===========

(defn run-directives [context m]
  (reduce-kv run context m))


;; =========== AS ===========

(defn as [associate-as entity]
  (if (context/meta-result? entity)
    (vary-meta entity (fn [context]
                        (context/update-primary context entity/override-association associate-as)))
    entity))
