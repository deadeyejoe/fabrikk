(ns fabrikk.build-context.beta.core
  (:require [fabrikk.build-graph.interface.gamma :as value-graph]
            [fabrikk.entity.interface :as entity]
            [fabrikk.build-context.interface :as build-context]
            [loom.graph :as graph]
            [medley.core :as medley]))

(defn init [primary]
  (-> (value-graph/build)
      (value-graph/add-node (entity/id primary) primary)
      (assoc :primary (entity/id primary))))

(defn id->entity [build-context id]
  (value-graph/node-value build-context id))

(defn primary [{:keys [primary] :as build-context}]
  (id->entity build-context primary))

(defn update-entity [build-context id f args]
  (apply value-graph/update-node build-context id f args))

(defn update-primary [{:keys [primary] :as build-context} f args]
  (update-entity build-context primary f args))

(defn associate-entity
  ([build-context label target-entity]
   (associate-entity build-context (primary build-context) label target-entity))
  ([build-context source-entity label target-entity]
   (let [source-id (entity/id source-entity)
         target-id (entity/id target-entity)
         associate-as (entity/associate-as target-entity)
         value-to-assoc (entity/value-to-assoc target-entity associate-as)]
     (-> build-context
         (value-graph/assoc-node target-id target-entity)
         (value-graph/update-edge source-id
                                  target-id
                                  (fn [edge] (assoc edge label associate-as)))
         (value-graph/update-node source-id entity/update-value assoc label value-to-assoc)))))

(defn associate-context
  ([build-context label target-context]
   (associate-context build-context (primary build-context) label target-context))
  ([build-context source-entity label target-context]
   (let [target-primary (primary target-context)]
     (-> build-context
         (value-graph/merge target-context)
         (associate-entity source-entity label target-primary)))))

(defn changed? [entity other]
  (not= (entity/value entity) (entity/value other)))

(defn propagate-link [build-context source-id target-id pending-entity]
  (let [values (->> (value-graph/edge-value build-context source-id target-id)
                    (medley/map-vals (partial entity/value-to-assoc pending-entity)))]
    (value-graph/update-node build-context source-id
                             entity/update-value merge values)))

(defn propagate [build-context pending-entity]
  (let [existing-entity (id->entity build-context (entity/id pending-entity))]
    (if (changed? existing-entity pending-entity)
      (->> (graph/in-edges build-context (entity/id existing-entity))
           (reduce (fn [context [source-id target-id]]
                     (propagate-link context source-id target-id pending-entity))
                   build-context))
      build-context)))

(defn traverse-edge [build-context source label]
  (if (nil? source)
    (->> (graph/out-edges build-context (entity/id source))
         (some (fn [[source-id target-id :as _edge]]
                 (let [edge-value (value-graph/edge-value build-context source-id target-id)]
                   (when (contains? edge-value label)
                     (value-graph/node-value build-context target-id))))))
    nil))

(defn traverse-path [build-context path]
  (reduce (partial traverse-edge build-context)
          (primary build-context)
          path))

(defn path [build-context path]
  (reductions (partial traverse-edge build-context)
              (primary build-context)
              path))
