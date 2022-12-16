(ns fabrikk.persistence.core
  (:require [fabrikk.entity.interface :as entity]))


(defonce options (atom {:default-method :store}))

(defn set-default-persistence [value]
  (swap! options assoc :default-method value))

(defn default-persistence-method []
  (:default-method @options))

;; ============ PERSIST! ============

(defn persist-dispatch-value [entity {:keys [persist-with] :as _create-opts}]
  (assert (entity/factory-id entity) "Cannot persist without a factory id!")
  (let [method (or persist-with
                   (entity/persist-with entity)
                   (default-persistence-method))]
    (cond
      (= :store method) :store
      method [method (entity/factory-id entity)]
      :else (entity/factory-id entity))))

(defn value-with-dispatch-meta [entity create-opts]
  (let [dispatch-value (persist-dispatch-value entity create-opts)]
    (tap> [::dispatch dispatch-value])
    (with-meta (entity/value entity) {::dispatch dispatch-value
                                      ::factory-id (entity/factory-id entity)})))

(defn dispatch-from-meta
  "Don't want to have to pass the entire entity to consumers of persist!, just the value
   So prior to calling persist! we'll add the actual dispatch value as metadata
   
   This feels a little janky, but we're in control of invocation of persist!, can add the
   meta just prior to calling it and discard the value immediately, and it's not required 
   for any further logic since we'll have the full entity in our code."
  [value]
  (-> value meta ::dispatch))

(defmulti persist!
  "Persists an entity. Uses the persistence method under the `:persist-with` key
   on the entity, or the default method if none is present (see `set-default-persistence`).
   Dispatches on [method-kw factory-id-kw].
   
   There is a default persistence method that stores entities in an atom see `store`
   in a vectory keyed to their factory-id"
  #'dispatch-from-meta)

(defmethod persist! :default [_entity]
  (throw (IllegalArgumentException. "No default persistence method found")))

;; ============ DEFAULT STORE ============

(defonce store (atom {}))

(def collect-entity (fnil conj []))

(defn store! [value]
  (let [factory-id (-> value meta ::factory-id)]
    (-> (swap! store update factory-id collect-entity value)
        factory-id
        last)))

(defn reset-store! []
  (reset! store {}))

(defmethod persist! :store [entity]
  (store! entity))
