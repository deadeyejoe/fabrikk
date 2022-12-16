(ns fabrikk.build-context.core
  (:require [fabrikk.build-graph.interface :as build-graph]
            [fabrikk.entity.interface :as entity]
            [loom.alg :as graph-alg]
            [loom.graph :as graph]
            [medley.core :as medley]))

(defn init [primary]
  (-> (build-graph/build)
      (build-graph/add-node (entity/id primary) primary)
      (assoc :primary (entity/id primary))))

(defn id->entity [build-context id]
  (build-graph/node-value build-context id))

(defn primary [{:keys [primary] :as build-context}]
  (id->entity build-context primary))

(defn update-entity [build-context id f args]
  (apply build-graph/update-node build-context id f args))

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
         (build-graph/insert-node target-id target-entity)
         (build-graph/update-edge source-id
                                  target-id
                                  (fn [edge] (assoc edge label associate-as)))
         (build-graph/update-node source-id entity/update-value assoc label value-to-assoc)))))

(defn associate-context
  ([build-context label target-context]
   (associate-context build-context (primary build-context) label target-context))
  ([build-context source-entity label target-context]
   (let [target-primary (primary target-context)]
     (-> build-context
         (build-graph/merge target-context)
         (associate-entity source-entity label target-primary)))))

(defn changed? [entity other]
  (tap> [:changed? entity other
         (not= (entity/value entity) (entity/value other))])
  (not= (entity/value entity) (entity/value other)))

(defn propagate-link [build-context source-id target-id pending-entity]
  (tap> [:link build-context source-id target-id pending-entity])
  (let [values (->> (build-graph/edge-value build-context source-id target-id)
                    (medley/map-vals (partial entity/value-to-assoc pending-entity)))]
    (tap> [:values (build-graph/edge-value build-context source-id target-id) values])
    (build-graph/update-node build-context source-id
                             entity/update-value merge values)))

(defn propagate [build-context pending-entity]
  (tap> [:entity build-context pending-entity])
  (let [existing-entity (id->entity build-context (entity/id pending-entity))]
    (if (changed? existing-entity pending-entity)
      (let [updated-context (build-graph/set-node build-context (entity/id pending-entity) pending-entity)]
        (->> (graph/in-edges updated-context (entity/id existing-entity))
             (reduce (fn [context [source-id target-id]]
                       (propagate-link context source-id target-id pending-entity))
                     updated-context)))
      build-context)))

(defn traverse-edge [build-context source label]
  (when source
    (->> (graph/out-edges build-context (entity/id source))
         (some (fn [[source-id target-id :as _edge]]
                 (let [edge-value (build-graph/edge-value build-context source-id target-id)]
                   (tap> [build-context _edge edge-value])
                   (when (contains? edge-value label)
                     (build-graph/node-value build-context target-id))))))))

(defn traverse-path [build-context path]
  (reduce (partial traverse-edge build-context)
          (primary build-context)
          path))

(defn path [build-context path]
  (reductions (partial traverse-edge build-context)
              (primary build-context)
              path))

(defn entities-in-build-order [{:keys [primary] :as build-context}]
  (if-let [sorted-ids (graph-alg/topsort build-context primary)]
    (->> sorted-ids
         (reverse)
         (map (partial id->entity build-context)))
    (throw (IllegalArgumentException. "Build graph must be a DAG"))))
