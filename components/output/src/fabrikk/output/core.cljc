(ns fabrikk.output.core
  (:require [fabrikk.execution-context.interface :as context]
            [fabrikk.entity.interface :as entity]))

(defn build-dispatch-fn [context output-opts]
  (or (:output-as output-opts)
      (-> context context/primary entity/factory :output-as)
      :meta))

(defmulti build #'build-dispatch-fn)

(defmethod build :meta [context _output-opts]
  (context/->result-meta context))

(defn collect-value [context factory-id value]
  (update context
          (-> factory-id name keyword)
          (fnil conj []) value))

(defmethod build :collection [context _output-opts]
  (->> (context/entities-in-build-order context)
       ;; TODO return result-meta here?
       (map (juxt entity/factory-id entity/value))
       (reduce (partial apply collect-value)
               {})))

(defmethod build :value [context {:keys [transform] :or {transform identity} :as _output-opts}]
  (-> context context/primary entity/value transform))

(defmethod build :tuple [context {:keys [transform] :or {transform identity} :as _output-opts}]
  [(-> context context/primary entity/value transform) context])

(defn as-meta []
  {:output-as :meta})

(defn as-collection []
  {:output-as :collection})

(defn as-tuple []
  {:output-as :tuple})

(defn as-value [transform]
  (cond-> {:output-as :value}
    transform (assoc :transform transform)))
