(ns fabrikk.execution.core
  (:require [fabrikk.build-context.interface :as context]
            [fabrikk.directive-core.interface :as directive-core]
            [fabrikk.directory.interface :as directory]
            [fabrikk.entity.interface :as entity]
            [fabrikk.factory.interface :as factory]
            [fabrikk.output.interface :as output]
            [fabrikk.persistence.interface :as persistence]
            [fabrikk.template.interface :as template]
            [fabrikk.utils.interface :as utils]
            [superstring.core :as str])
  (:import java.lang.Exception))

(defn remove-transients [context transients]
  (cond-> context
    (seq transients) (context/update-primary entity/update-value #(apply dissoc % transients))))

(defn resolve-factory [factory]
  (cond
    (factory/factory? factory) factory
    (directory/resolve-factory factory) (directory/resolve-factory factory)
    :else (throw (IllegalArgumentException. (str "Unrecognised factory: " factory)))))

(defn after-build-fn [after-build-config]
  (if after-build-config
    (fn [context]
      (context/update-primary context
                              entity/update-value (partial after-build-config context)))
    identity))

(defn build-entity [execution-context
                    {:as factory
                     :keys [before-build after-build transients]
                     :or {before-build identity }}
                    build-opts]
  (let [effective-template (factory/compile-template factory build-opts)
        after (after-build-fn after-build)]
    (-> effective-template
        (before-build)
        (template/execute directive-core/run execution-context)
        (after)
        (remove-transients transients))))

(defn build-context [factory build-opts]
  (let [resolved (resolve-factory factory)
        context (context/init (entity/create! resolved build-opts))]
    (build-entity context resolved build-opts)))

(defn build [factory build-opts output-opts]
  (output/build (build-context factory build-opts) output-opts))

(defn coerce-to-list [build-opt-list+]
  (cond
    (map? build-opt-list+) [build-opt-list+]
    (coll? build-opt-list+) (vec build-opt-list+)
    :else [{}]))

(defn build-many [factory number build-opt+]
  (->> (coerce-to-list build-opt+)
       (utils/pad-with-last number)
       (map (partial build-context factory))))

(defn build-list-context 
  "The output of this should be:
   
   First: an inspectable list of entities (i.e. not just their primary id or assoc-as values).
   
   Second: a list that can be passed to other builds. The directives code can handle replacing
   the list entries with the correct assoc-as value.
   
   Third: a list that can be destructured into contextful entities. These entities can in turn
   be passed as dependents in other builds.
   
   With this in mind we do something weird looking: the return value is a list of contextful entities."
  [factory number build-opt+]
  (let [contexts (build-many factory number build-opt+)
        list-value  (mapv output/->result-meta contexts)
        list-context (reduce (partial apply context/associate-context)
                             (context/init (entity/create-list!))
                             (map-indexed vector contexts))]
     ;;update the value after the context/associate calls, overwriting the list of assoc-as values
    (context/update-primary list-context entity/update-value (constantly list-value))))

(defn build-list [factory n build-opt+ output-opts]
  (output/build (build-list-context factory n build-opt+) output-opts))

(defn wrap-persist-error [context entity exception]
  (let [full-path (context/path-to context entity)
        display-path (map first full-path)]
    (ex-info (str "Error persisting at path: "
                  (str/join " -> " display-path)
                  ", " (ex-message exception))
             {:value (entity/value entity)
              :full-path full-path}
             exception)))

(defn persist-and-propagate! [output-opts context entity-id]
  (let [current-entity (context/id->entity context entity-id)]
    (if (entity/needs-persist? current-entity)
      (let [value-with-dispatch (persistence/value-with-dispatch-meta current-entity output-opts)
            persisted-value (try
                              (persistence/persist! (entity/factory-id current-entity) value-with-dispatch)
                              (catch Exception e
                                (throw (wrap-persist-error context current-entity e))))
            persisted-entity (entity/set-persisted-value current-entity persisted-value)]
        (context/propagate context persisted-entity))
      context)))

(defn persist-context [output-opts built-context]
  (reduce (partial persist-and-propagate! output-opts)
          built-context

          (->> (context/entities-in-build-order built-context)
               (map :uuid))))

(defn create [factory build-opts output-opts]
  (output/build
   (->> (build-context factory build-opts)
        (persist-context output-opts))
   output-opts))

(defn create-list [factory n build-opt-list output-opts]
  (output/build
   (->> (build-list-context factory n build-opt-list)
        (persist-context output-opts))
   output-opts))
