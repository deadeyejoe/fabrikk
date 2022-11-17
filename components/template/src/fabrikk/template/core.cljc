(ns fabrikk.template.core
  (:require [clojure.spec.alpha :as s])
  (:refer-clojure :exclude [compile]))

(s/def ::field keyword?)
(s/def ::value any?)
(s/def ::template-map (s/map-of ::field ::value))
(s/def ::template-tuple (s/cat :field ::field :value ::value))
(s/def ::fragment (s/or :map ::template-map
                        :tuple ::template-tuple))
(s/def ::fragment-list (s/coll-of ::fragment :kind sequential?))
(s/def ::instance (s/or :map ::template-map
                        :list (s/coll-of ::fragment)))

(defn init []
  {:ordering []
   :field->tuple {}})

(defn coerce-to-tuples [fragment]
  (cond
    (s/valid? ::template-tuple fragment) [fragment]
    (s/valid? ::template-map fragment) (into [] fragment)
    :else (throw (IllegalArgumentException. (str "Invalid template fragment" fragment)))))

(defn coerce-description-to-list [description]
  (cond
    (s/valid? ::template-map description) [description]
    (s/valid? ::template-tuple description) [description]
    (s/valid? ::fragment-list description) description
    :else (throw (IllegalArgumentException. (str "Invalid template " description)))))

(defn add-tuple [{:keys [ordering] :as template} [field _value :as tuple]]
  (let [new? (= -1 (.indexOf ordering field))]
    (cond-> (assoc-in template [:field->tuple field] tuple)
      new? (update :ordering conj field))))

(defn combine [tuples]
  (reduce add-tuple (init) tuples))

(defn compile [descriptions]
  (->> descriptions
       (mapcat coerce-description-to-list)
       (mapcat coerce-to-tuples)
       (combine)))

(defn execute [{:keys [ordering field->tuple] :as _template} f init-ctx]
  (reduce (fn [ctx field]
            (apply f ctx (get field->tuple field)))
          init-ctx
          ordering))
