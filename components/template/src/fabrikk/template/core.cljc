(ns fabrikk.template.core
  (:require [clojure.spec.alpha :as s]
            [fabrikk.directive-core.interface :as directive-core])
  (:refer-clojure :exclude [compile exists?]))

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
  {:pre-ordering []
   :ordering []
   :post-ordering []
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

(defn exists? [{:keys [pre-ordering ordering post-ordering] :as _template} field]
  (or (= -1 (.indexOf pre-ordering field))
      (= -1 (.indexOf ordering field))
      (= -1 (.indexOf post-ordering field))))

(defn ordering-collection [[_field value :as tuple]]
  (or
   (when (directive-core/directive? value)
     (tap> [::ordering value])
     (case (:ordering value)
       :pre :pre-ordering
       :post :post-ordering
       nil))
   :ordering))

(defn add-tuple [template [field _value :as tuple]]
  (let [new? (exists? template field)
        collection-to-update (ordering-collection tuple)]
    (cond-> (assoc-in template [:field->tuple field] tuple)
      new? (update collection-to-update conj field))))

(defn combine [tuples]
  (reduce add-tuple (init) tuples))

(defn compile [descriptions]
  (->> descriptions
       (mapcat coerce-description-to-list)
       (mapcat coerce-to-tuples)
       (combine)))

(defn execute [{:keys [pre-ordering ordering post-ordering field->tuple] :as _template} f init-ctx]
  (reduce (fn [ctx field]
            (apply f ctx (get field->tuple field)))
          init-ctx
          (concat pre-ordering
                  ordering
                  post-ordering)))
