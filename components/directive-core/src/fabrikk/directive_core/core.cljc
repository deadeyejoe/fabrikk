(ns fabrikk.directive-core.core
  (:require [clojure.spec.alpha :as s]
            [fabrikk.entity.interface :as entity]
            [fabrikk.execution-context.interface :as context]))

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

(defn run-directive-dispatch [_ _ directive]
  (get directive directive-type-kw))

(defmulti run #'run-directive-dispatch)

(defn meta->context [value]
  (if (sequential? value)
    (->> value
         (map meta)
         (map-indexed vector)
         (reduce (partial apply context/associate)
                 (context/init (entity/create-list!))))
    (meta value)))

(defmethod run :default [build-context key value]
  (if (context/meta-result? value)
    (context/associate build-context key (meta->context value))
    (context/assoc-value build-context key (if (fn? value)
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
