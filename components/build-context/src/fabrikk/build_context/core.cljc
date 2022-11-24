(ns fabrikk.build-context.core
  (:require [fabrikk.build-graph.interface.beta :as build-graph]
            [fabrikk.entity.interface :as entity]
            [loom.graph :as graph]
            [fabrikk.build-context.interface :as build-context])
  (:refer-clojure :exclude [merge]))

(defrecord BuildContext [primary id->entity id->link graph])
(defn init [primary]
  (let [id (entity/id primary)]
    (->BuildContext id
                    {id primary}
                    {}
                    (-> (build-graph/init)
                        (build-graph/add-node id)))))

(defn entity [{:keys [id->entity] :as _build-context} id]
  (get id->entity id))

(defn update-entity [build-context id f args]
  (apply update-in build-context [:id->entity id] f args))

(defn primary [{:keys [primary] :as build-context}]
  (entity build-context primary))

(defn update-primary [{:keys [primary] :as build-context} f args]
  (update-entity build-context primary f args))

(defn merge [build-context {:keys [id->entity id->link graph] :as _to-merge}]
  (-> build-context
      (update :id->entity merge-with entity/combine-no-conflict id->entity)
      (update :id->link clojure.core/merge id->link)
      (update :graph build-graph/merge graph)))

(defn associate-entity [{:keys [primary graph] :as build-context} label entity]
  (let [associate-as (entity/associate-as entity)
        value-to-assoc (entity/value-to-assoc entity associate-as)
        [updated-graph new-edge-id] (build-graph/add-edge graph primary (entity/id entity))]
    (-> build-context
        (assoc :graph updated-graph)
        (assoc-in [:id->link new-edge-id] {:source primary
                                           :label label
                                           :associate-as associate-as
                                           :target (entity/id entity)})
        (update-primary entity/update-value [assoc label value-to-assoc]))))

(defn associate-context [build-context label target-context]
  (let [target-primary (primary target-context)]
    (-> build-context
        (merge target-context)
        (associate-entity label target-primary))))

(defn propagate-link [build-context {:keys [source label associate-as target] :as _link}]
  (let [value (entity/value-to-assoc (entity build-context target) associate-as)]
    (update-entity build-context source entity/update-value [assoc label value])))

(defn edge->links [{:keys [graph id->link] :as _build-context} [source target :as _edge]]
  (map (fn [edge-id]
         (assoc (id->link edge-id)
                :source source
                :target target))
       (build-graph/edge-ids-between graph source target)))

(defn propagate [{:keys [graph] :as build-context} entity]
  (->> (graph/in-edges graph (entity/id entity))
       (mapcat (partial edge->links build-context))
       (reduce propagate-link build-context)))
