(ns extrude.execution.core
  (:require [extrude.directive.interface :as directive]
            [extrude.entity.interface :as entity]
            [extrude.execution-context.interface :as context]
            [extrude.factory.interface :as factory]))

(defn remove-transients [entity transients]
  (apply dissoc entity (or (keys transients) [])))

(defn build-entity [execution-context
                    {:as factory
                     :keys [before-build after-build transients]
                     :or {before-build identity after-build identity}}
                    opts]
  (let [effective-template (factory/combine-traits-and-templates factory opts)]
    (-> effective-template
        (before-build)
        ((partial directive/run-directives execution-context))
        (after-build)
        (remove-transients transients))))

(defn build [factory opts]
  (let [context (-> (context/init)
                    (context/set-primary! (entity/create! factory {})))]
    (build-entity context factory opts)))

(defn create [factory opts])
