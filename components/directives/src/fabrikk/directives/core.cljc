(ns fabrikk.directives.core
  (:require [fabrikk.build-context.interface :as context]
            [fabrikk.directive-core.interface :as core]
            [fabrikk.directory.interface :as directory]
            [fabrikk.entity.interface :as entity]
            [fabrikk.execution.interface :as execution]
            [fabrikk.factory.interface :as factory]
            [fabrikk.specs.interface :as specs]
            [clojure.spec.alpha :as s])
  (:refer-clojure :exclude [sequence derive])
  (:import java.lang.IllegalArgumentException))

(s/def ::directive map?)

(defn assoc-value [context key value]
  (context/update-primary context entity/update-value assoc key value))

;; =========== CONSTANT ===========

(defn constant [x]
  (core/->directive ::constant
                    {:value x}))
(s/fdef constant
  :args (s/cat :x any?)
  :ret ::core/directive)

(defmethod core/run ::constant [context key {:keys [value] :as _directive}]
  (assoc-value context key value))

;; =========== SEQUENCE ===========

(defonce sequence-cache (atom {}))

(defn reset-sequence! []
  (reset! sequence-cache {}))

(comment
  (reset-sequence!))

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
    (assoc-value context key (value next-number))))

;; =========== BUILD ===========

(defn factory->id [factory]
  (cond
    (factory/factory? factory) (:id factory)
    (directory/resolve-factory factory) factory
    :else (throw (IllegalArgumentException. (str "Unrecognised factory: " factory)))))

(defn build
  ([factory] (build factory {}))
  ([factory build-opts]
   (core/->directive ::build
                     {:value (factory->id factory)
                      :build-opts build-opts
                      :ordering :pre})))

(defmethod core/run ::build [context key {:keys [value build-opts] :as _directive}]
  (context/associate-context context
                             key
                             (execution/build-context value build-opts)))

;; =========== BUILD LIST ===========

(defn build-list
  ([factory n] (build-list factory n [{}]))
  ([factory n build-opt+]
   (core/->directive ::build-list
                     {:value (factory->id factory)
                      :number n
                      :build-opt+ build-opt+
                      :ordering :pre})))

(defmethod core/run ::build-list [context key
                                  {:keys [value number build-opt+]
                                   :or {number 0}
                                   :as _directive}]
  (let [list-values (execution/build-many value number build-opt+)
        list-context (reduce (partial apply context/associate-context)
                             (context/init (entity/create-list!))
                             (map-indexed vector list-values))]
    (context/associate-context context key list-context)))

;; =========== DERIVE ===========

(defn derive
  ([key-or-path f]
   (core/->directive ::derive
                     {:value key-or-path
                      :transform f
                      :ordering :post})))

(defmethod core/run ::derive [context key {:keys [transform] derive-from :value :as _directive}]
  (let [source-entity (if (sequential? derive-from)
                        (context/traverse-path context derive-from)
                        (context/primary context))
        assoc-as (if (sequential? derive-from)
                   transform
                   (comp transform derive-from))]
    (context/associate-entity context key (entity/override-association source-entity assoc-as))))
