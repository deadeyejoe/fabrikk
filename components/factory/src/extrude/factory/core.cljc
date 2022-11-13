(ns extrude.factory.core
  (:require [extrude.specs.interface :as specs]
            [clojure.spec.alpha :as s]))

(defn ->factory [factory] factory)
(s/fdef ->factory
  :ret ::specs/factory)

(defn combine-traits-and-templates [{:keys [template traits id]}
                                    {:keys [with] selected-traits :traits :as opts}]
  (merge template
         (apply merge (-> traits (select-keys selected-traits) vals))
         id
         with))
