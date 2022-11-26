(ns fabrikk.directives.core
  (:require [fabrikk.directive-core.interface :as core]
            [fabrikk.entity.interface :as entity]
            [fabrikk.execution.interface :as execution]
            [fabrikk.execution-context.interface :as context]
            [fabrikk.specs.interface :as specs]
            [clojure.spec.alpha :as s]
            [fabrikk.factory.interface :as factory])
  (:refer-clojure :exclude [sequence derive]))

(s/def ::directive map?)

;; =========== CONSTANT ===========

(defn constant [x]
  (core/->directive ::constant
                    {:value x}))
(s/fdef constant
  :args (s/cat :x any?)
  :ret ::core/directive)

(defmethod core/run ::constant [context key {:keys [value] :as _directive}]
  (context/assoc-value context key value))

;; =========== SEQUENCE ===========

(defonce sequence-cache (atom {}))

(defn reset-sequence! []
  (reset! sequence-cache {}))

(defn sequence
  "Creates a directive that returns a sequence of numbers that may be transformed by
   an optional function 'f'. The sequence is scoped to the primary factory by default.
   
   Passing an identifier allows the scope to be shared in multiple contexts. A nil identifier
   or an unavailable factory result in a global scope"
  ([f identifier]
   (core/->directive ::sequence
                     {:value f
                      :identifier identifier})))
(s/fdef sequence
  :args (s/cat :f ::specs/transformer :identifier (s/nilable keyword?))
  :ret ::core/directive)

(defn increment-number [cache sequence-key]
  (update cache sequence-key (fnil inc 0)))

(defn get-number [cache sequence-key]
  (get cache sequence-key))

(defmethod core/run ::sequence [context key {:keys [value identifier]}]
  (let [sequence-key (or identifier
                         (-> (context/primary context)
                             (entity/factory-id))
                         :global)
        next-number  (-> (swap! sequence-cache increment-number sequence-key)
                         (get-number sequence-key))]
    (context/assoc-value context key (value next-number))))

;; =========== BUILD ===========

(defn coerce-factory [factory]
  (cond
    (factory/factory? factory) (:id factory)
    (factory/resolve factory) factory
    :else (throw (IllegalArgumentException. (str "Unrecognised factory: " factory)))))

(defn build
  ([factory] (build factory {}))
  ([factory build-opts]
   (core/->directive ::build
                     {:value (coerce-factory factory)
                      :build-opts build-opts
                      :ordering :pre})))

(defmethod core/run ::build [context key {:keys [value build-opts] :as _directive}]
  (context/associate context
                     key
                     (execution/build-context value build-opts)))

;; =========== BUILD LIST ===========

(defn build-list
  ([factory n] (build-list factory n [{}]))
  ([factory n build-opt-list]
   (core/->directive ::build-list
                     {:value (coerce-factory factory)
                      :number n
                      :build-opt-list build-opt-list
                      :ordering :pre})))

(defn coerce-to-list [build-opt-list]
  (cond
    (map? build-opt-list) [build-opt-list]
    (coll? build-opt-list) (vec build-opt-list)
    :else [{}]))

(defmethod core/run ::build-list [context key
                                  {:keys [value number build-opt-list]
                                   :or {number 0}
                                   :as _directive}]
  (context/associate context
                     key
                     (execution/build-list-context value number (coerce-to-list build-opt-list))))

;; =========== DERIVE ===========

(defn derive
  ([key-or-path f]
   (core/->directive ::derive
                     {:value key-or-path
                      :transform f
                      :ordering :post})))

(defmethod core/run ::derive [context key {:keys [transform] derive-from :value :as directive}]
  (let [source-entity (if (sequential? derive-from)
                        (context/path context derive-from)
                        (context/primary context))
        assoc-as (if (sequential? derive-from)
                   transform
                   (comp transform derive-from))]
    (context/associate-entity context key (entity/override-association source-entity assoc-as))))
