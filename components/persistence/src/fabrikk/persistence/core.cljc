(ns fabrikk.persistence.core
  (:require [fabrikk.entity.interface :as entity]))


(defonce options (atom {:default-method :store}))

(defn set-default-persistence [value]
  (swap! options assoc :default-method value))

(defn default-persistence-method []
  (:default-method @options))

;; ============ PERSIST! ============

(defn persist-dispatch-fn [entity _value]
  (assert (entity/factory-id entity) "Cannot persist without a factory id!")
  (let [method (or (entity/persist-with entity)
                   (default-persistence-method))]
    (cond
      (= :store method) :store
      method [method (entity/factory-id entity)]
      :else (entity/factory-id entity))))

(defmulti persist!
  "Persists an entity. Uses the persistence method under the `:persist-with` key
   on the entity, or the default method if none is present (see `set-default-persistence`).
   Dispatches on [method-kw factory-id-kw].
   
   There is a default persistence method that stores entities in an atom see `store`
   in a vectory keyed to their factory-id"
  #'persist-dispatch-fn)

(defmethod persist! :default [_entity _value]
  (throw (IllegalArgumentException. "No default persistence method found")))

;; ============ DEFAULT STORE ============

(defonce store (atom {}))

(def collect-entity (fnil conj []))

(defn store! [entity]
  (let [factory-id (entity/factory-id entity)]
    (swap! store update factory-id collect-entity (entity/value entity))
    entity))

(defn reset-store! []
  (reset! store {}))

(defmethod persist! :store [entity]
  (store! entity))
