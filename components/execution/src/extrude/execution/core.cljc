(ns extrude.execution.core
  (:require [extrude.directive.interface :as directive]
            [extrude.entity.interface :as entity]
            [extrude.execution-context.interface :as context]
            [extrude.factory.interface :as factory]
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
                    opts]
  (tap> [::build-entity factory])
  (let [effective-template (factory/combine-traits-and-templates factory opts)]
    (-> effective-template
        (before-build)
        ((partial directive/run-directives execution-context))
        (after-build)
        (remove-transients transients))))

(defn build-context [factory opts]
  (let [resolved (resolve-factory factory)
        context (-> (entity/create! resolved) 
                    (context/init))]
    (build-entity context resolved opts)))

(defn build [factory opts]
  (context/->result-meta (build-context factory opts)))

(defn build-list-context [factory number opt-list]
  (->> (utils/pad-with-last number opt-list)
       (map-indexed (fn [index build-opts]
                      [index (build-context factory build-opts)]))
       (reduce (fn [current-list [index built-context]]
                 (-> (context/associate current-list index built-context)
                     (context/update-value conj (context/->result-meta built-context))))
               (context/->list-context))))

(defn build-list [factory n opt-list]
  (context/->result-meta (build-list-context factory n opt-list)))

(defn persist-and-store! [context entity])

(defn create [factory opts]
  (let [built-context (build-context factory opts)
        entities-in-order (->> (context/entities-in-build-order built-context)
                               (filter entity/needs-persist?))]
    (reduce persist-and-store!
            built-context
            entities-in-order)))

(defn create-list [factory opts opt-list])
