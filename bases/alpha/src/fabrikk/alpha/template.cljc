(ns fabrikk.alpha.template 
  (:require [fabrikk.template.interface :as template])
  (:refer-clojure :exclude [exists?]))

(defn exists? [template field]
  (template/exists? template field))

(defn new? [template field]
  (template/new? template field))

(defn value [template field]
  (template/value template field))

(defn insert [template field value]
  (template/insert template field value))

(defn update-existing [template field value]
  (template/update-existing template field value))

(defn upsert [template field value]
  (template/upsert template field value))

(defn without [template attributes]
  (template/without template attributes))
