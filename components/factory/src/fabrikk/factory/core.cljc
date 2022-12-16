(ns fabrikk.factory.core
  (:require [fabrikk.specs.interface :as specs]
            [fabrikk.directory.interface :as directory]
            [fabrikk.template.interface :as template]
            [clojure.spec.alpha :as s]))

(defn ->factory [{:keys [id] :as description}]
  (let [factory (merge {:persistable true}
                       description)]
    (directory/register-factory id factory)
    (with-meta factory {::factory true})))
(s/fdef ->factory
  :ret ::specs/factory)

(defn factory? [x]
  (::factory (meta x)))

(defn compile-template [{:keys [template traits] :as _factory}
                        {:keys [with without] selected-traits :traits :as _opts}]
  (cond-> (template/compile (-> [template]
                                (into (map traits selected-traits))
                                (into (if with [with] []))))
    without (template/without without)))
