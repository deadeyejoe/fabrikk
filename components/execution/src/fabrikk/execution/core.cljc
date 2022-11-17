(ns fabrikk.execution.core
  (:require [fabrikk.directive-core.interface :as directive-core]
            [fabrikk.entity.interface :as entity]
            [fabrikk.execution-context.interface :as context]
            [fabrikk.factory.interface :as factory]
            [fabrikk.persistence.interface :as persistence]
            [fabrikk.utils.interface :as utils]))

(defn remove-transients [context transients]
  (cond-> context
    (seq transients) (context/update-primary entity/update-value #(apply dissoc % transients))))

(defn resolve-factory [factory]
  (cond
    (factory/factory? factory) factory
    (factory/resolve factory) (factory/resolve factory)
    :else (throw (IllegalArgumentException. (str "Unrecognised factory: " factory)))))

(defn after-build-fn [after-build-config]
  (if after-build-config
    (fn [context] (context/update-primary context entity/update-value after-build-config))
    identity))

(defn build-entity [execution-context
                    {:as factory
                     :keys [before-build after-build transients]
                     :or {before-build identity }}
                    build-opts]
  (let [effective-template (factory/combine-traits-and-templates factory build-opts)
        after (after-build-fn after-build)]
    (-> effective-template
        (before-build)
        ((partial directive-core/run-directives execution-context))
        (after)
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

(defn coerce-to-list [build-opt-list]
  (cond
    (map? build-opt-list) [build-opt-list]
    (list? build-opt-list) build-opt-list
    :else [{}]))

(defn build-list [factory n build-opt-list]
  (->> (coerce-to-list build-opt-list)
       (map assoc-as-list-item)
       (build-list-context factory n)
       (context/->result-meta)))

(defn unchanged? [entity persisted-entity]
  (= (entity/value entity) (entity/value persisted-entity)))
(def changed? (complement unchanged?))

(defn persist-and-propagate! [create-opts context entity-id]
  (let [entity (context/entity context entity-id)]
    (if (entity/needs-persist? entity)
      (let [value-with-dispatch (persistence/value-with-dispatch-meta entity create-opts) 
            persisted-value (persistence/persist! value-with-dispatch)
            persisted-entity (entity/set-persisted-value entity persisted-value)]
        (if (changed? entity persisted-entity)
          (-> context
              (context/set-entity persisted-entity)
              (context/propagate persisted-entity))
          context))
      context)))

(defn persist-context [create-opts built-context]
  (reduce (partial persist-and-propagate! create-opts)
          built-context
          (context/entities-in-build-order built-context)))

(defn create [factory build-opts create-opts]
  (->> (build-context factory build-opts)
       (persist-context create-opts)
       (context/->result-meta)))

(defn create-list [factory n build-opt-list create-opts]
  (->> (coerce-to-list build-opt-list)
       (map assoc-as-list-item)
       (build-list-context factory n)
       (persist-context create-opts)
       (context/->result-meta)))
