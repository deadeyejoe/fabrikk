(ns fabrikk.factory.core
  (:require [clojure.spec.alpha :as s]
            [fabrikk.directory.interface :as directory]
            [fabrikk.execution.interface :as execution]
            [fabrikk.output.interface :as output]
            [fabrikk.specs.interface :as specs])
  (:import java.lang.IllegalArgumentException))

(defn ->factory [{:keys [id] :as description}]
  (let [factory (merge {:persistable true}
                       description)]
    (directory/register-factory id factory)
    (with-meta factory {::factory true})))
(s/fdef ->factory
  :ret ::specs/factory)

(defn factory? [x]
  (::factory (meta x)))

(defn resolve-factory [factory-or-kw]
  (cond
    (factory? factory-or-kw) factory-or-kw
    (directory/resolve-factory factory-or-kw) (directory/resolve-factory factory-or-kw)
    :else (throw (IllegalArgumentException. (str "Unrecognised factory: " factory-or-kw)))))

(defn ->id [factory-or-kw]
  (cond
    (factory? factory-or-kw) (:id factory-or-kw)
    (directory/resolve-factory factory-or-kw) factory-or-kw
    :else (throw (IllegalArgumentException. (str "Unrecognised factory: " factory-or-kw)))))

(defn build [factory build-opts output-opts]
  (let [resolved (resolve-factory factory)]
    (output/build (execution/build-context resolved build-opts) output-opts)))
(s/fdef build
  :args (s/cat :factory (s/or :instance ::specs/factory
                              :reference qualified-keyword?)
               :build-opts (s/nilable ::specs/build-opts)
               :output-opts (s/nilable ::specs/output-opts)))

(defn build-list [factory n build-opt+ output-opts]
  (output/build (execution/build-list-context factory n build-opt+) output-opts))

(defn create [factory build-opts output-opts]
  (output/build
   (->> (execution/build-context factory build-opts)
        (execution/persist-context output-opts))
   output-opts))

(defn create-list [factory n build-opt-list output-opts]
  (output/build
   (->> (execution/build-list-context factory n build-opt-list)
        (execution/persist-context output-opts))
   output-opts))
