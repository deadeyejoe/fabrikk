(ns fabrikk.template.core
  (:require [clojure.spec.alpha :as s])
  (:refer-clojure :exclude [compile exists?])
  (:import java.lang.IllegalArgumentException))

(s/def ::field keyword?)
(s/def ::value any?)
(s/def ::template-map (s/map-of ::field ::value))
(s/def ::template-tuple (s/cat :field ::field :value ::value))
(s/def ::fragment (s/or :map ::template-map
                        :tuple ::template-tuple))
(s/def ::fragment-list (s/coll-of ::fragment :kind sequential?))
(s/def ::description (s/or :map ::template-map
                           :list ::fragment-list))

(s/def ::ordering (s/coll-of ::field :kind sequential?))
(s/def ::field->tuple (s/map-of ::field ::template-tuple))
(s/def ::compiled (s/keys :req [::ordering
                                ::field->tuple]))

(defn init []
  {::ordering []
   ::field->tuple {}})

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

(declare new?)

(defn update-tuple [template [field _value :as tuple]]
  (assoc-in template [::field->tuple field] tuple))

(defn add-tuple [template [field _value :as tuple]]
  (let [new? (new? template field)]
    (cond-> (update-tuple template tuple)
      new? (update ::ordering conj field))))

(defn compile [descriptions]
  (->> descriptions
       (mapcat coerce-description-to-list)
       (mapcat coerce-to-tuples)
       (reduce add-tuple (init))))

(defn combine [template description]
  (->> (coerce-description-to-list description)
       (mapcat coerce-to-tuples)
       (reduce add-tuple template)))

(defn execute [{:keys [::ordering ::field->tuple] :as _template} f init-ctx]
  (reduce (fn [ctx field]
            (apply f ctx (get field->tuple field)))
          init-ctx
          ordering))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public interface
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn exists? [{:keys [::ordering] :as _template} field]
  (< -1 (.indexOf ordering field)))

(def new? (complement exists?))

(defn without [template attributes]
  (-> template
      (update ::field->tuple #(apply dissoc % attributes))
      (update ::ordering (partial remove (set attributes)))))

(defn insert [template field value]
  (cond-> template 
   (new? template field) (add-tuple [field value])))

(defn update-existing [template field value]
  (cond-> template
    (exists? template field) (update-tuple [field value])))

(defn upsert [template field value]
  (if (exists? template field)
    (update-tuple template [field value])
    (add-tuple template [field value])))
