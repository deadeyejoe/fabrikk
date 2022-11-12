(ns extrude.directive.standard
  (:require [extrude.directive.core :as core]
            [clojure.spec.alpha :as s]))

(s/def ::directive map?)

;; =========== CONSTANT ===========

(defn constant [x]
  (core/->directive ::constant
                    {:value x}))
(s/fdef constant
  :args (s/cat :x any?)
  :ret ::directive)

(defmethod core/run ::constant [{:keys [value] :as _directive}]
  value)

(comment
  (let [pre (fn [d] (update d :value #(str "before" %)))
        post #(str % "after")]
    (map core/evaluate [(constant "x")
                        (-> (constant "x")
                            (core/before pre))
                        (-> (constant "x")
                            (core/after post))
                        (-> (constant "x")
                            (core/before pre)
                            (core/after post))]))
  (core/evaluate "X")
  (core/evaluate (fn [] "X")))

;; =========== BUILD ===========

(defn build
  ([factory] (build factory {}))
  ([factory build-opts]
   (core/->directive ::build
                     {:value factory
                      :build-opts build-opts})))

(defmethod core/run ::build [{:keys [value build-opts]}]
  (throw "Not implemented"))

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

(defmethod core/run ::build-list [{:keys [value number build-opt-list]
                                   :or {number 0}
                                   :as _directive}]
  (throw "Not implemented"))

;; =========== PROVIDE ===========

(defn provide [build-context]
  (core/->directive ::provide
                    {:value build-context}))

(defmethod core/run ::provide [{sub-context :value}]
  (throw "Not implemented"))
