(ns fabrikk.execution.core
  (:require [fabrikk.directive-core.interface :as directive-core]
            [fabrikk.entity.interface :as entity]
            [fabrikk.execution-context.interface :as context]
            [fabrikk.factory.interface :as factory]
            [fabrikk.persistence.interface :as persistence]
            [fabrikk.utils.interface :as utils]))

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
        ((partial directive-core/run-directives execution-context))
        (after-build)
        (remove-transients transients))))

(defn build-context [factory build-opts]
  (let [resolved (resolve-factory factory)
        context (context/init (entity/create! resolved build-opts))]
    (build-entity context resolved build-opts)))

(defn build [factory build-opts]
  (context/->result-meta (build-context factory build-opts)))

(defn build-list-context [factory number build-opt-list]
  (let [list-context (context/init (entity/create-list!))]
    (->> (utils/pad-with-last number build-opt-list)
         (map-indexed (fn [index build-opts]
                        [index (build-context factory build-opts)]))
         (reduce (partial apply context/associate)
                 list-context))))

(defn assoc-as-list-item [build-opts]
  (assoc build-opts :as :list-item))

(defn build-list [factory n build-opt-list]
  (->> (map assoc-as-list-item build-opt-list)
       (build-list-context factory n)
       (context/->result-meta)))

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

(defn persist-context [built-context]
  (reduce persist-and-propagate!
          built-context
          (context/entities-in-build-order built-context)))

(defn create [factory opts]
  (-> (build-context factory opts)
      (persist-context)
      (context/->result-meta)))

(defn create-list [factory n build-opt-list]
  (->> (map assoc-as-list-item build-opt-list)
       (build-list-context factory n)
       (persist-context)
       (context/->result-meta)))
