(ns extrude.execution.core
  (:require [extrude.directive.interface :as directive]
            [extrude.entity.interface :as entity]
            [extrude.execution-context.interface :as context]
            [extrude.factory.interface :as factory]
            [extrude.persistence.interface :as persistence]
            [extrude.utils.interface :as utils]))

(defn remove-transients [entity transients]
  (apply dissoc entity (or (keys transients) [])))

(defn resolve-factory [factory]
  (cond
    (factory/factory? factory) factory
    (factory/resolve factory) (factory/resolve factory)
    :else (throw (IllegalArgumentException. (str "Unrecognised factory: " factory)))))

(defn build-entity [execution-context
                    {:as factory
                     :keys [before-build after-build transients]
                     :or {before-build identity after-build identity}}
                    build-opts]
  (let [effective-template (factory/combine-traits-and-templates factory build-opts)]
    (-> effective-template
        (before-build)
        ((partial directive/run-directives execution-context))
        (after-build)
        (remove-transients transients))))

(defn build-context [factory build-opts]
  (let [resolved (resolve-factory factory)
        context (context/init (entity/create! resolved build-opts))]
    (build-entity context resolved build-opts)))

(defn build [factory build-opts]
  (context/->result-meta (build-context factory build-opts)))

(defn build-list-context [factory number opt-list]
  (let [list-context (context/init (entity/create-list!))]
    (->> (utils/pad-with-last number opt-list)
         (map-indexed (fn [index build-opts]
                        [index (build-context factory build-opts)]))
         (reduce (partial apply context/associate)
                 list-context))))

(defn build-list [factory n opt-list]
  (context/->result-meta (build-list-context factory n opt-list)))

(defn unchanged? [entity persisted-entity]
  (= (entity/value entity) (entity/value persisted-entity)))
(def changed? (complement unchanged?))

(defn persist-and-propagate! [context entity-id]
  (let [entity (context/entity context entity-id)]
    (if (entity/needs-persist? entity)
      (let [persisted-entity (persistence/persist! entity)]
        (if (changed? entity persisted-entity)
          (-> context
              (context/set-entity persisted-entity)
              (context/propagate persisted-entity))
          context))
      context)))

(defn create-context [factory opts]
  (let [built-context (build-context factory opts)
        ordered-entity-ids (context/entities-in-build-order built-context)]
    (reduce persist-and-propagate!
            built-context
            ordered-entity-ids)))

(defn create [factory opts]
  (context/->result-meta (create-context factory opts)))

(defn create-list [factory opts opt-list])
