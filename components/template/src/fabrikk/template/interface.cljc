(ns fabrikk.template.interface
  (:require [fabrikk.template.core :as template])
  (:refer-clojure :exclude [compile update]))

(defn update [template field value]
  (template/add-tuple template [field value]))

(defn compile [descriptions]
  (template/compile descriptions))

(defn without [template attributes]
  (template/without template attributes))

(defn execute [template f init-ctx]
  (template/execute template f init-ctx))
