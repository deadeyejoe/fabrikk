(ns fabrikk.factory.core
  (:require [fabrikk.specs.interface :as specs]
            [clojure.spec.alpha :as s])
  (:refer-clojure :exclude [resolve]))

(def directory (atom {}))

(defn resolve [id]
  (get @directory id))

(defn ->factory [{:keys [id] :as description}]
  (let [factory (merge {:persistable true}
                       description)]
    (swap! directory assoc id factory)
    (with-meta factory {::factory true})))
(s/fdef ->factory
  :ret ::specs/factory)

(defn factory? [x]
  (::factory (meta x)))

(defn combine-traits-and-templates [{:keys [template traits] :as factory}
                                    {:keys [with] selected-traits :traits :as opts}]
  (merge template
         (apply merge (-> traits (select-keys selected-traits) vals))
         with))
