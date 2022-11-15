(ns extrude.entity.core
  (:require [extrude.specs.interface :as specs]
            [clojure.spec.alpha :as s]))

(s/def ::uuid uuid?)
(s/def ::value any?)
(s/def ::persisted boolean?)
(s/def ::instance (s/keys :opt-un [::specs/factory
                                   ::persisted
                                   ::uuid
                                   ::value]))



(defn create! [factory build-opts]
  {:uuid (random-uuid)
   :factory factory
   :persisted false
   :build-opts build-opts
   :value {}})

(defn create-list! []
  {:uuid (random-uuid)
   :persisted false
   :value []})

(defn refresh-uuid [entity]
  (assoc entity :uuid (random-uuid)))

(defn uuid-match? [entity other-entity]
  (= (:uuid entity) (:uuid other-entity)))
(def uuid-mismatch? (complement uuid-match?))

(defn value-match? [entity other-entity]
  (= (:value entity) (:value other-entity)))
(def value-mismatch? (complement value-match?))

(defn persisted-match? [entity other-entity]
  (= (:persisted entity) (:persisted other-entity)))
(def persisted-mismatch? (complement persisted-match?))

(defn conflict? [entity other-entity]
  (and (uuid-match? entity other-entity)
       (persisted-match? entity other-entity)
       (value-mismatch? entity other-entity)))

(def no-conflict? (complement conflict?))

(defn assert-no-conflict [entity other-entity]
  (assert (no-conflict? entity other-entity) "Conflict!"))

(defn pick-persisted
  "Assumes only one is persisted"
  [entity other-entity]
  (if (:persisted entity)
    entity
    other-entity))

(defn combine [entity other-entity]
  (when (uuid-match? entity other-entity)
    (if (persisted-match? entity other-entity)
      (when (value-match? entity other-entity)
        other-entity)
      (pick-persisted entity other-entity))))

(defn combine-no-conflict [entity other-entity]
  (assert-no-conflict entity other-entity)
  (combine entity other-entity))

(comment
  (let [one (create! {} {})
        two (assoc one :persisted true)]
    [(uuid-match? one two)
     (persisted-match? one two)
     (value-match? one two)
     (combine one two)]))

(def factory :factory)

(defn factory-id [entity]
  (-> entity :factory :id))

(defn associate-as [entity]
  (or (-> entity :associate-as)
      (-> entity :build-opts :as)
      (-> entity :factory :primary-id)))

(defn override-association [entity associate-as]
  (assoc entity :associate-as associate-as))

(def value :value)

(defn update-value [entity f args]
  (apply update entity :value f args))

(def persisted? :persisted)

(defn persistable? [entity]
  (-> entity :factory :persistable))

(defn needs-persist? [entity]
  (and (persistable? entity)
       (persisted?)))
