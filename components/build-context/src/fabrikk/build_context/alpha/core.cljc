(ns fabrikk.build-context.alpha.core
  (:require [fabrikk.build-graph.interface.beta :as build-graph]
            [fabrikk.entity.interface :as entity]
            [loom.graph :as graph])
  (:refer-clojure :exclude [merge]))

(defrecord BuildContext [primary id->entity id->link graph])
(defn init [primary]
  (let [id (entity/id primary)]
    (->BuildContext id
                    {id primary}
                    {}
                    (-> (build-graph/init)
                        (build-graph/add-node id)))))

(defn coerce-to-id [entity-or-id]
  (or (entity/id entity-or-id) entity-or-id))

(defn entity [{:keys [id->entity] :as _build-context} entity-or-id]
  (get id->entity (coerce-to-id entity-or-id)))

(defn assert-entity! [build-context entity-or-id]
  (assert (entity build-context (coerce-to-id entity-or-id))
          "Entity not present in graph!"))

(defn update-entity [build-context id f args]
  (apply update-in build-context [:id->entity id] f args))

(defn primary [{:keys [primary] :as build-context}]
  (entity build-context primary))

(defn update-primary [{:keys [primary] :as build-context} f args]
  (update-entity build-context primary f args))

(defn assert-link-equal [link other]
  (if (and link other)
    (assert (= link other) (str "Non matching links found in merge: " link ", " other))
    (or link other)))

(defn merge [build-context {:keys [id->entity id->link graph] :as _to-merge}]
  (-> build-context
      (update :id->entity (partial merge-with entity/combine-no-conflict) id->entity)
      (update :id->link (partial merge-with assert-link-equal) id->link)
      (update :graph build-graph/merge graph)))

(defn associate-entity
  ([build-context label target-entity]
   (associate-entity build-context (primary build-context) label target-entity))
  ([{:keys [graph] :as build-context} source-entity label target-entity]
   (assert-entity! build-context source-entity)
   (let [source-id (entity/id source-entity)
         target-id (entity/id target-entity)
         associate-as (entity/associate-as target-entity)
         value-to-assoc (entity/value-to-assoc target-entity associate-as)
         [updated-graph new-edge-id] (build-graph/add-edge graph source-id target-id)]
     (-> build-context
         (assoc :graph updated-graph)
         (update-in [:id->entity (entity/id target-entity)] entity/combine-no-conflict target-entity)
         (update-in [:id->link new-edge-id] assert-link-equal {:label label
                                                               :associate-as associate-as})
         (update-entity source-id entity/update-value [assoc label value-to-assoc])))))

(defn associate-context
  ([build-context label target-context] (associate-context build-context (primary build-context) label target-context))
  ([build-context source-entity label target-context]
   (let [target-primary (primary target-context)]
     (-> build-context
         (merge target-context)
         (associate-entity source-entity label target-primary)))))

(defn propagate-link [build-context {:keys [source label pending-value persisted-value] :as _link-context}]
  (if (= pending-value persisted-value)
    build-context
    (update-entity build-context source entity/update-value [assoc label persisted-value])))

(defn edge->link-context [{:keys [graph id->link] :as _build-context} pending-entity persisted-entity [source target :as _edge]]
  (map (fn [edge-id]
         (let [{:keys [associate-as] :as link} (id->link edge-id)
               pending-value (entity/value-to-assoc pending-entity associate-as)
               persisted-value (entity/value-to-assoc persisted-entity associate-as)]
           (assoc link
                  :pending-value pending-value
                  :persisted-value persisted-value
                  :source source
                  :target target)))
       (build-graph/edge-ids-between graph source target)))

(defn changed? [entity other]
  (not= (entity/value entity) (entity/value other)))

(defn propagate [{:keys [graph] :as build-context} persisted-entity]
  (let [pending-entity (entity build-context (entity/id persisted-entity))]
    (if (changed? pending-entity persisted-entity)
      (->> (graph/in-edges graph (entity/id persisted-entity))
           (mapcat (partial edge->link-context build-context pending-entity persisted-entity))
           (reduce propagate-link build-context))
      build-context)))

(defn traverse-label [{:keys [graph id->link] :as _build-context} source label]
  (->> (graph/out-edges graph source)
       (some (fn [[source target :as _edge]]
               (->> (build-graph/edge-ids-between graph source target)
                    (some (fn [edge-id]
                            (when (= label (-> edge-id id->link :label))
                              target))))))))

(defn path [build-context path]
  (reduce (fn [entity-id fragment]
            (or (traverse-label build-context entity-id fragment)
                (reduced nil)))
          (-> build-context primary entity/id)
          path))
