(ns fabrikk.build-graph.alpha.core
  (:require [clojure.spec.alpha :as s]
            [fabrikk.build-graph.link-graph :as link-graph]
            [fabrikk.entity.interface :as entity]
            [loom.graph :as graph]
            [loom.alg :as graph-alg]))

(s/def ::primary ::entity/instance)
(s/def ::codex (s/map-of ::entity/uuid ::entity/instance))
(s/def ::instance (s/keys :opt-un [::graph
                                   ::primary
                                   ::codex]))

(defn self-loop? [[src dest :as edge]]
  ;; TODO: This is a hack, clean this up
  (= src dest))

(def not-self-loop? (complement self-loop?))

(defrecord BuildGraph [codex labels]
  ;; TODO: clean this shit up. Either delegate to link graph implementation, or change link graph implementation
  graph/Graph
  (edges [_]
    (mapcat (fn [[src label->dest]]
              (->> label->dest
                   (map second)
                   (map (partial vector src))
                   (filterv not-self-loop?)))
            labels))
  (has-edge? [_ src dest]
    (->> (get labels src)
         (vals)
         (map second)
         (some (partial = dest))))
  (nodes [_]
    (keys codex))
  (has-node? [_ node]
    (get codex node))
  (out-degree [_ src-node]
    (count (get labels src-node)))
  (out-edges [_ src-node]
    (->> (get labels src-node)
         (vals)
         (map second)
         (map (partial vector src-node))
         (filterv not-self-loop?)))
  (successors* [x src-node]
    (map second (graph/out-edges x src-node)))
  graph/Digraph
  (in-edges [_ dest-node]
    (->>
     (link-graph/inward-links labels dest-node)
     (map (juxt first last))
     (filterv not-self-loop?)))
  (in-degree [x dest-node]
    (count (graph/in-edges x dest-node)))
  (predecessors* [x dest-node]
    (map first (graph/out-edges x dest-node)))
  (transpose [_]
    (throw (Exception. "Not implemented"))))

(defn in-edges-with-links [{:keys [labels] :as _bg} node]
  (link-graph/inward-links labels node))

(declare set-primary!)

(defn init
  ([]
   (assoc (->BuildGraph {} {})
          :primary nil))
  ([entity] (-> (init)
                (set-primary! entity))))

(defn ensure-node [{:keys [codex] :as build-graph} {:keys [uuid] :as entity}]
  (if-let [existing (get codex uuid)]
    (assoc-in build-graph [:codex uuid] (entity/combine-no-conflict existing entity))
    (assoc-in build-graph [:codex uuid] entity)))

(defn assert-node [{:keys [codex] :as _bg} {:keys [uuid] :as _entity}]
  (assert (get codex uuid) "Entity was not found in the graph!"))

(defn set-primary! [build-graph {:keys [uuid] :as entity}]
  (-> build-graph
      (ensure-node entity)
      (assoc :primary uuid)))

(defn entity [build-graph id]
  (get-in build-graph [:codex id]))

(defn update-entity [build-graph id f args]
  (apply update-in build-graph [:codex id] f args))

(defn update-entity-value [build-graph id f args]
  (apply update-in build-graph [:codex id :value] f args))

(defn primary [{:keys [primary] :as build-graph}]
  (entity build-graph primary))

(defn update-primary [{:keys [primary] :as build-graph} f args]
  (update-entity build-graph primary f args))

(defn update-primary-value [{:keys [primary] :as build-graph} f args]
  (update-entity-value build-graph primary f args))

(defn merge-builds [{:keys [labels codex] :as primary} to-merge]
  (-> primary
      (assoc :codex (merge-with entity/combine-no-conflict codex (:codex to-merge)))
      (assoc :labels (link-graph/merge labels (:labels to-merge)))))

(defn link-entities [build-graph entity-id [_label _associate :as link] other-entity-id]
  (update build-graph :labels link-graph/link entity-id link other-entity-id))

(defn associate [{:keys [primary] :as build-graph}
                 link
                 {associated-primary :primary :as associated-build-graph}]
  (-> build-graph
      (merge-builds associated-build-graph)
      (link-entities primary link associated-primary)))

(defn add-link [{:keys [primary] :as build-graph}
                link
                {:keys [uuid] :as entity}]
  (assert-node build-graph entity)
  (link-entities build-graph primary link uuid))

(defn path
  "Given a build graph and a path comprised of a sequence of labels. Starting at the 
   primary node, traverse edges with each label in turn, and return the node at the end of
   the path, if it exists. Or nil otherwise"
  [{:keys [codex labels primary] :as _build-graph} path]
  (get codex (link-graph/traverse-path labels primary path)))

(defn entities-in-build-order [{:keys [codex primary] :as build-graph}]
  (if-let [sorted-ids (graph-alg/topsort build-graph primary)]
    (map codex (reverse sorted-ids))
    (throw (IllegalArgumentException. "Build graph must be a DAG"))))

(comment
  (-> (->BuildGraph {1 :one 2 :two}
                    {1 {:org 2}})
      (graph-alg/topsort)))
