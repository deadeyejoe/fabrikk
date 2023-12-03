(ns fabrikk.directive-core.core
  (:require [fabrikk.build-context.interface :as context]
            [fabrikk.entity.interface :as entity]
            [fabrikk.output.interface :as output]
            [clojure.spec.alpha :as s]))

(s/def ::type qualified-keyword?)
(s/def ::value any?)
(s/def ::directive (s/keys :req [::type]
                           :opt-un [::value]))

;; =========== CREATION ===========

(def directive-type-kw ::type)

(defn ->directive
  ([type-kw] (->directive type-kw {}))
  ([type-kw opts]
   (merge {directive-type-kw type-kw} opts)))
(s/fdef ->directive
  :args (s/cat :type-kw qualified-keyword? :opts (s/? map?))
  :ret ::directive)

;; =========== RUN ===========

(defn assoc-value [build-context k v]
  (context/update-primary build-context
                          entity/update-value assoc k v))

(defn run-directive-dispatch [_ _ directive]
  (get directive directive-type-kw))

(defmulti run #'run-directive-dispatch)

(defn meta->context [value]
  (if (sequential? value)
    (->> value
         (map meta)
         (map-indexed vector)
         (reduce (partial apply context/associate-context)
                 (context/init (entity/create-list!))))
    (meta value)))

(defmethod run :default [build-context key value]
  (if (output/meta-result? value)
    (context/associate-context build-context key (meta->context value))
    (assoc-value build-context key (if (fn? value)
                                     (value)
                                     value))))

;; =========== DIRECTIVE ===========

(defprotocol Directive
  (evaluate [this build-context key]))

(defn directive? [m]
  (or (and (map? m)
           (contains? m directive-type-kw))
      (satisfies? Directive m)))

(comment
  (directive? nil)
  (defrecord Constant [value]
    Directive
    (evaluate [_x build-context key]
      (assoc-value build-context key value)))
  (evaluate (->Constant 1) {} :foo))

;; =========== UTILS ===========

(defn run-directives [context m]
  (reduce-kv run context m))


;; =========== AS ===========

(defn as [associate-as entity]
  (if (output/meta-result? entity)
    (vary-meta entity (fn [context]
                        (context/update-primary context entity/override-association associate-as)))
    entity))
