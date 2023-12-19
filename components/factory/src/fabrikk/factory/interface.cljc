(ns fabrikk.factory.interface
  (:require [clojure.spec.alpha :as s]
            [fabrikk.factory.core :as core]
            [fabrikk.specs.interface :as specs]))

(s/def ::factory-or-kw (s/or :instance ::core/instance
                             :reference qualified-keyword?))
(s/def ::one-or-more-build-opts (s/or :single ::specs/build-opts
                                      :many (s/coll-of ::specs/build-opts)))

(defn ->factory [factory-description]
  (core/->factory factory-description))
(s/fdef ->factory
  :args (s/cat :description ::specs/factory-description)
  :ret ::core/instance)

(defn factory? [x]
  (core/factory? x))

(defn resolve-factory [factory]
  (core/resolve-factory factory))

(defn ->id [factory]
  (core/->id factory))

(defn build [factory build-opts output-opts]
  (core/build factory build-opts output-opts))
(s/fdef build
  :args (s/cat :factory ::factory-or-kw
               :build-opts ::specs/build-opts
               :output-opts ::specs/output-opts))

(defn build-list [factory n build-opt+ output-opts]
  (core/build-list factory n build-opt+ output-opts))
(s/fdef build-list
  :args (s/cat :factory ::factory-or-kw
               :quantity pos-int?
               :build-opts ::one-or-more-build-opts
               :output-opts ::specs/output-opts))

(defn create [factory build-opts output-opts]
  (core/create factory build-opts output-opts))
(s/fdef create
  :args (s/cat :factory ::factory-or-kw
               :build-opts ::specs/build-opts
               :output-opts ::specs/output-opts))

(defn create-list [factory n build-opt-list output-opts]
  (core/create-list factory n build-opt-list output-opts))
(s/fdef create-list
  :args (s/cat :factory ::factory-or-kw
               :quantity pos-int?
               :build-opts ::one-or-more-build-opts
               :output-opts ::specs/output-opts))
