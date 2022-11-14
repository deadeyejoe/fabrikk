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

(defn pad-with-last [n coll]
  (let [last-element (last coll)]
    (->> (concat coll (repeat last-element))
         (take n))))

(comment
  (pad-with-last -1 [1 2 3])
  (pad-with-last 1 [1 2 3])
  (pad-with-last 2 [1 2 3])
  (pad-with-last 3 [1 2 3])
  (pad-with-last 10 [1 2 3])
  (pad-with-last 3 [{}])
  (pad-with-last 3 []))

(defn populate-list [list-context number factory build-opt-list]
  (->> (pad-with-last number build-opt-list)
       (map-indexed (fn [index build-opts]
                      [index (execution/build-context factory build-opts)]))
       (reduce (fn [current-list [index built-context]]
                 (-> (context/associate current-list index built-context)
                     (context/update-value conj (context/->result-meta built-context))))
               list-context)))

(defmethod core/run ::build-list [context key
                                  {:keys [value number build-opt-list]
                                   :or {number 0}
                                   :as _directive}]
  (let [list-context (-> (entity/create! value [])
                         (context/entity->context))
        populated-list-context (populate-list list-context
                                              number
                                              value
                                              build-opt-list)]
    (link-to-context context key populated-list-context)))

;; =========== PROVIDE ===========

(defn provide [build-context]
  (core/->directive ::provide
                    {:value build-context}))

(defmethod core/run ::provide [context key {provided :value}]
  (if-let [provided-context (meta provided)]
    (link-to-context context key provided-context)
    (context/assoc-value context key provided)))
