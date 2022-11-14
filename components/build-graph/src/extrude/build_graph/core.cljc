(ns extrude.build-graph.core
  (:require [extrude.entity.interface :as entity]
            [extrude.build-graph.label-graph :as label-graph]
            [clojure.spec.alpha :as s]))

(s/def ::primary ::entity/instance)
(s/def ::codex (s/map-of ::entity/uuid ::entity/instance))
(s/def ::instance (s/keys :opt-un [::graph
                                   ::primary
                                   ::codex]))

(defn init []
  {:labels {}
   :codex {}
   :primary nil})

(defn ensure-node [{:keys [codex] :as bg} {:keys [uuid] :as entity}]
  (if-let [existing (get codex uuid)]
    (assoc-in bg [:codex uuid] (entity/combine-no-conflict existing entity))
    (assoc-in bg [:codex uuid] entity)))

(defn set-primary! [bg {:keys [uuid] :as entity}]
  (-> bg
      (ensure-node entity)
      (assoc :primary uuid)))

(defn entity->build-graph [entity]
  (-> (init)
      (ensure-node entity)
      (set-primary! entity)))

(defn link [bg entity-id label other-entity-id] 
  (update bg :labels label-graph/link entity-id label other-entity-id))

(defn merge-builds [{:keys [labels codex] :as primary} to-merge]
  (-> primary
      (assoc :codex (merge-with entity/combine-no-conflict codex (:codex to-merge)))
      (assoc :labels (label-graph/merge labels (:labels to-merge)))))

(defn associate [{:keys [primary] :as build-graph} 
                 label
                 {associated-primary :primary :as associated-build-graph}]
  (-> build-graph
      (merge-builds associated-build-graph)
      (link primary label associated-primary)))

(defn path
  "Given a build graph and a path comprised of a sequence of labels. Starting at the 
   primary node, traverse edges with each label in turn, and return the node at the end of
   the path, if it exists. Or nil otherwise"
  [{:keys [codex labels primary] :as _build-graph} path]
  (get codex (label-graph/traverse-path labels primary path)))
