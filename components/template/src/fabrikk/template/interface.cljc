(ns fabrikk.template.interface
  (:require [clojure.spec.alpha :as s]
            [fabrikk.template.core :as template]
            [fabrikk.template.interface.specs :as template-specs])
  (:refer-clojure :exclude [compile exists? merge]))

(defn compile [description]
  (template/compile description))
(s/fdef compile
  :args (s/cat :description ::template-specs/description)
  :ret (s/and ::template-specs/compiled
              template/consistent?))

(defn combine [template description]
  (template/combine template description))
(s/fdef combine
  :args (s/cat :template ::template-specs/compiled
               :description ::template-specs/description)
  :ret ::template-specs/compiled)

(defn merge [template-1 template-2]
  (template/merge template-1 template-2))
(s/fdef merge
  :args (s/cat :template-1 ::template-specs/compiled
               :template-2 ::template-specs/compiled)
  :ret ::template-specs/compiled)

(defn execute [template f init-ctx]
  (template/execute template f init-ctx))
(s/fdef execute
  :args (s/cat :template ::template-specs/compiled
               :f ifn?
               :init-ctx any?)
  :ret any?)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public interface
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::arg-pair (s/cat :template ::template-specs/compiled
                         :field ::template-specs/field))

(defn exists? [template field]
  (template/exists? template field))
(s/fdef exists?
  :args ::arg-pair
  :ret boolean?)

(defn new? [template field]
  (template/new? template field))
(s/fdef new?
  :args ::arg-pair
  :ret boolean?)

(defn value [template field]
  (template/value template field))
(s/fdef value
  :args ::arg-pair)

(s/def ::arg-triple (s/cat :template ::template-specs/compiled
                           :field ::template-specs/field
                           :value any?))

(defn insert [template field value]
  (template/insert template field value))
(s/fdef insert
  :args ::arg-triple
  :ret ::template-specs/compiled)

(defn update-existing [template field value]
  (template/update-existing template field value))
(s/fdef update-existing
  :args ::arg-triple
  :ret ::template-specs/compiled)

(defn upsert [template field value]
  (template/upsert template field value))
(s/fdef upsert
  :args ::arg-triple
  :ret ::template-specs/compiled)

(defn without [template attributes]
  (template/without template attributes))
(s/fdef without
  :args (s/cat :template ::template-specs/compiled
               :attributes (s/coll-of ::template-specs/field))
  :ret ::template-specs/compiled)
