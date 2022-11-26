(ns fabrikk.entity.test-utils
  (:require [fabrikk.entity.interface :as entity]))

(def default-factory
  {:id ::factory})

(defn entity-with-value
  ([value] (entity-with-value default-factory value))
  ([factory value] (-> (entity/create! factory {})
                       (entity/update-value (constantly value)))))
