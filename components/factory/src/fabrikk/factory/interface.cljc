(ns fabrikk.factory.interface
  (:require [clojure.spec.alpha :as s]
            [fabrikk.factory.core :as core]
            [fabrikk.specs.interface :as specs]))

(defn ->factory [factory]
  (core/->factory factory))

(defn factory? [x]
  (core/factory? x))

(defn resolve-factory [factory]
  (core/resolve-factory factory))

(defn ->id [factory]
  (core/->id factory))

(defn build [factory build-opts output-opts]
  (core/build factory build-opts output-opts))
(s/fdef build
  :args (s/cat :factory (s/or :instance ::specs/factory
                              :reference qualified-keyword?)
               :build-opts (s/nilable ::specs/build-opts)
               :output-opts (s/nilable ::specs/output-opts)))

(defn build-list [factory n build-opt+ output-opts]
  (core/build-list factory n build-opt+ output-opts))

(defn create [factory build-opts output-opts]
  (core/create factory build-opts output-opts))

(defn create-list [factory n build-opt-list output-opts]
  (core/create-list factory n build-opt-list output-opts))
