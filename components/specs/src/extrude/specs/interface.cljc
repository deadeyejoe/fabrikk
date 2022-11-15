(ns extrude.specs.interface
  (:require [clojure.spec.alpha :as s]))

(s/def ::thunk (s/fspec :args empty?))
(s/def ::transformer (s/fspec :args (s/cat :input any?)))

(s/def ::id qualified-keyword?)
(s/def ::factory-id ::id)
(s/def ::template (s/map-of keyword? any?))
(s/def ::primary-id keyword?)
(s/def ::persistable boolean?)
(s/def ::traits (s/map-of keyword? ::template))
(s/def ::transients ::template)
(s/def ::before-build ::transformer)
(s/def ::after-build ::transformer)
(s/def ::factory (s/keys :req-un [::id
                                  ::template]
                         :opt-un [::primary-id
                                  ::persistable
                                  ::traits
                                  ::transients
                                  ::before-build
                                  ::after-build]))
