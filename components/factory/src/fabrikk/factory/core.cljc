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
  #?@(:clj
      (clojure.lang.IFn
       (invoke [this] (build this {} {}))
       (invoke [this opts] (build this opts {}))
       (applyTo [this args] (apply build this args)))
      :cljs
      (cljs.core/IFn
       (-invoke [this] (build this {} {}))
       (-invoke [this opts] (build this opts {})))))

(s/def ::template ::template-specs/compiled)
(s/def ::instance (s/and (partial instance? Factory)
                         (s/keys :req-un [::template])))

(defn ->factory [description]
  (let [{:keys [id] :as factory} (->> (update description :template template/compile)
                                      (merge {:id (random-uuid)
                                              :persistable true})
                                      (map->Factory))]
    (directory/register-factory id factory)
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

(defn inherit [factory-or-kw {:keys [id template traits] :as description}]
  (let [resolved (resolve-factory factory-or-kw)
        inherited (-> resolved
                      (merge {:id (random-uuid)} (dissoc description :template :traits))
                      (update :template template/combine template)
                      (update :traits merge traits)
                      (assoc :parent (->id resolved)))]
    (directory/register-factory id inherited)
    inherited))

(defn mould [factory-or-kw build-opts]
  (let [resolved (resolve-factory factory-or-kw)
        moulded (assoc resolved
                       :id (random-uuid)
                       :template (execution/compile-template resolved build-opts))]
    (directory/register-factory (:id moulded) moulded)
    moulded))

(defn build [factory-or-kw build-opts output-opts]
  (let [resolved (resolve-factory factory-or-kw)]
    (output/build (execution/build-context resolved build-opts) output-opts)))

(defn build-list [factory-or-kw n build-opt+ output-opts]
  (let [resolved (resolve-factory factory-or-kw)]
    (output/build (execution/build-list-context resolved n build-opt+) output-opts)))

(defn create [factory-or-kw build-opts output-opts]
  (let [resolved (resolve-factory factory-or-kw)]
    (output/build
     (->> (execution/build-context resolved build-opts)
          (execution/persist-context output-opts))
     output-opts)))

(defn create-list [factory-or-kw n build-opt-list output-opts]
  (let [resolved (resolve-factory factory-or-kw)]
    (output/build
     (->> (execution/build-list-context resolved n build-opt-list)
          (execution/persist-context output-opts))
     output-opts)))
