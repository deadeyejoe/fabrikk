(ns extrude.specs.interface
  (:require [clojure.spec.alpha :as s]))

(s/def ::thunk (s/fspec :args empty?))
(s/def ::transformer (s/fspec :args (s/cat :input any?)))


(s/def ::entity keyword?)
(s/def ::template (s/map-of keyword? any?))
(s/def ::primary-id keyword?)
(s/def ::traits (s/map-of keyword? ::template))
(s/def ::transients ::template)
(s/def ::before-build ::transformer)
(s/def ::after-build ::transformer)
(s/def ::factory (s/keys :req-un [::entity
                                  ::template]
                         :opt-un [::primary-id
                                  ::traits
                                  ::transients
                                  ::before-build
                                  ::after-build]))
