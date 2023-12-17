(ns fabrikk.template.interface
  (:require [fabrikk.template.core :as template]
            [clojure.spec.alpha :as s])
  (:refer-clojure :exclude [compile exists?]))

(defn compile [descriptions]
  (template/compile descriptions))
(s/fdef compile
  :args (s/cat :descriptions (s/coll-of ::template/description :kind sequential?))
  :ret ::template/compiled)

(defn combine [template description]
  (template/combine template description))
(s/fdef combine
  :args (s/cat :template ::template/compiled
               :description ::template/description)
  :ret ::template/compiled)

(defn execute [template f init-ctx]
  (template/execute template f init-ctx))
(s/fdef execute
  :args (s/cat :template ::template/compiled
               :f ifn?
               :init-ctx any?)
  :ret any?)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public interface
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn exists? [template field]
  (template/exists? template field))
(s/fdef exists?
  :args (s/cat :template ::template/compiled
               :field ::template/field)
  :ret boolean?)

(defn new? [template field]
  (template/new? template field))
(s/fdef new?
  :args (s/cat :template ::template/compiled
               :field ::template/field)
  :ret boolean?)

(s/def ::arg-triple (s/cat :template ::template/compiled
                           :field ::template/field
                           :value any?))

(defn insert [template field value]
  (template/insert template field value))
(s/fdef insert
  :args ::arg-triple
  :ret ::template/compiled)

(defn update-existing [template field value]
  (template/update-existing template field value))
(s/fdef update-existing
  :args ::arg-triple
  :ret ::template/compiled)

(defn upsert [template field value]
  (template/upsert template field value))
(s/fdef upsert
  :args ::arg-triple
  :ret ::template/compiled)

(defn without [template attributes]
  (template/without template attributes))
(s/fdef without
  :args (s/cat :template ::template/compiled
               :attributes (s/coll-of ::template/field))
  :ret ::template/compiled)
