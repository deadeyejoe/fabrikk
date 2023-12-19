(ns fabrikk.template.core
  (:require [clojure.spec.alpha :as s]
            [fabrikk.template.interface.specs :as template-specs])
  (:refer-clojure :exclude [compile exists? merge])
  (:import java.lang.IllegalArgumentException))

(defn init []
  {:ordering []
   :field->tuple {}})

(defn coerce-to-tuples [fragment]
  (cond
    (s/valid? ::template-specs/template-tuple fragment) [fragment]
    (s/valid? ::template-specs/template-map fragment) (into [] fragment)
    :else (throw (IllegalArgumentException. (str "Invalid template fragment" fragment)))))

(defn coerce-description-to-list [description]
  (cond
    (s/valid? ::template-specs/template-map description) [description]
    (s/valid? ::template-specs/template-tuple description) [description]
    (s/valid? ::template-specs/fragment-list description) description
    :else (throw (IllegalArgumentException. (str "Invalid template " description)))))

(declare new?)

(defn update-tuple [template [field _value :as tuple]]
  (assoc-in template [:field->tuple field] tuple))

(defn add-tuple [template [field _value :as tuple]]
  (let [new? (new? template field)]
    (cond-> (update-tuple template tuple)
      new? (update :ordering conj field))))

(defn compile [description]
  (->> description
       (mapcat coerce-description-to-list)
       (mapcat coerce-to-tuples)
       (reduce add-tuple (init))))

(defn combine [template description]
  (->> (coerce-description-to-list description)
       (mapcat coerce-to-tuples)
       (reduce add-tuple template)))

(defn merge [template-1 template-2]
  {:ordering  (->> (into (:ordering template-1) (:ordering template-2)) 
                   (distinct)
                   (vec))
   :field->tuple (clojure.core/merge (:field->tuple template-1) (:field->tuple template-2))})

(defn execute [{:keys [:ordering :field->tuple] :as _template} f init-ctx]
  (reduce (fn [ctx field]
            (apply f ctx (get field->tuple field)))
          init-ctx
          ordering))

(defn consistent? [{:keys [ordering field->tuple] :as _template}]
  (= (set ordering)
     (set (keys field->tuple))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public interface
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn exists? [{:keys [field->tuple] :as _template} field]
  (contains? field->tuple field))

(def new? (complement exists?))

(defn value [template field]
  (-> template :field->tuple field second))

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

(defn without [template attributes]
  (-> template
      (update :field->tuple #(apply dissoc % attributes))
      (update :ordering (partial remove (set attributes)))))
