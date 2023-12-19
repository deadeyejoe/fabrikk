(ns fabrikk.factory.core
  (:require [clojure.spec.alpha :as s]
            [fabrikk.directory.interface :as directory]
            [fabrikk.execution.interface :as execution]
            [fabrikk.output.interface :as output]
            [fabrikk.template.interface :as template]
            [fabrikk.template.interface.specs :as template-specs])
  (:import java.lang.IllegalArgumentException))

(declare build)

(defrecord Factory [id
                    template
                    primary-id
                    persistable
                    traits
                    transients
                    before-build
                    after-build]
  #?(:clj clojure.lang.IFn
     :cljs cljs.core/IFn)
  (invoke [this build-opts]
    (build this build-opts nil)))

(s/def ::template ::template-specs/compiled)
(s/def ::instance (s/and (partial instance? Factory)
                         (s/keys :req-un [::template])))

(defn ->factory [{:keys [id] :as description}]
  (let [factory (->> (update description :template template/compile)
                     (merge {:persistable true}) 
                     (map->Factory))]
    (when id (directory/register-factory id factory))
    factory))

(defn factory? [x]
  (instance? Factory x))

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
