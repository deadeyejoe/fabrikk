(ns fabrikk.output.core
  (:require [fabrikk.build-context.interface :as context]
            [fabrikk.entity.interface :as entity]
            [fabrikk.directory.interface :as directory])
  (:import java.lang.IllegalArgumentException))

(defn ->result [context]
  (-> context context/primary entity/value))

(defn ->result-meta [context]
  (let [bare-entity (->result context)]
    (with-meta bare-entity (assoc context ::meta true))))

(defn meta-result? [x]
  (-> x meta ::meta))

(defn build-dispatch-fn [context output-opts]
  (or (:output-as output-opts)
      (-> context context/primary entity/factory-id directory/resolve-factory :output-as)
      :meta))

(defmulti build #'build-dispatch-fn)

(defmethod build :meta [context _output-opts]
  (->result-meta context))

(defmethod build :value [context {:keys [transform] :or {transform identity} :as _output-opts}]
  (-> context context/primary entity/value transform))

(defmethod build :context [context _output-opts]
  context)

(defmethod build :tuple [context {:keys [transform] :or {transform identity} :as _output-opts}]
  [(-> context context/primary entity/value transform) context])

(defn collect-value [context factory-id value]
  (if-let [coll-name (some-> factory-id name keyword)] ;;un-namespace
    (update context coll-name (fnil conj []) value)
    context)) ;; if factory is nil, don't record it 

(defmethod build :collection [context _output-opts]
  (->> (context/entities-in-build-order context)
       ;; TODO return result-meta here?
       (map (juxt entity/factory-id entity/value))
       (reduce (partial apply collect-value)
               {})))

(defmethod build :path [_context {:keys [_paths] :as _output-opts}]
  (throw (new IllegalArgumentException "Not implemented")))

(defmethod build :build-order [context _output-opts]
  (->> (context/entities-in-build-order context)
       (filter entity/persistable?)
       (map entity/value)))

(defn as-meta []
  {:output-as :meta})

(defn as-value [transform]
  (cond-> {:output-as :value}
    transform (assoc :transform transform)))

(defn as-context []
  {:output-as :context})

(defn as-tuple []
  {:output-as :tuple})

(defn as-collection []
  {:output-as :collection})
