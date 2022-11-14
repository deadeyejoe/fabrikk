(ns extrude.directive.standard
  (:require [extrude.directive.core :as core]
            [extrude.execution-context.interface :as context]
            [clojure.spec.alpha :as s]
            [extrude.execution.interface :as execution]))

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

(defmethod core/run ::build [context key {:keys [value build-opts] :as _directive}]
  (tap> [::build context key _directive])
  (let [built (execution/build-context value build-opts)]
    (-> context
        (context/associate key built)
        (context/assoc-value key (context/->result-meta built)))))

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

(defmethod core/run ::build-list [context key
                                  {:keys [value number build-opt-list]
                                   :or {number 0}
                                   :as _directive}]
  (throw "Not implemented"))

;; =========== PROVIDE ===========

(defn provide [build-context]
  (core/->directive ::provide
                    {:value build-context}))

(defmethod core/run ::provide [context key {sub-context :value}]
  (throw "Not implemented"))
