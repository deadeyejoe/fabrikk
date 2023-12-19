(ns fabrikk.specs.interface
  (:require [clojure.spec.alpha :as s]))


(s/def ::id qualified-keyword?)
(s/def ::factory-id ::id)

(s/def ::entity-map map?)
(s/def ::entity-list coll?)
(s/def ::entity (s/or :entity-map ::entity-map
                      :entity-list ::entity-list))

;; Templates can either be a map, or a list of maps and/or tuples
(s/def ::template-map (s/map-of keyword? any?))
(s/def ::template-tuple (s/tuple keyword? any?))
(s/def ::template-list (s/coll-of (s/or :template-map ::template-map
                                        :template-tuple ::template-tuple)))
(s/def ::template (s/or :template-map ::template-map
                        :template-list ::template-list))

(s/def ::primary-id keyword?)
(s/def ::persistable boolean?)
(s/def ::traits (s/map-of keyword? ::template))
(s/def ::transients ::template)
(s/def ::before-build fn?)
(s/def ::after-build fn?)
(s/def ::factory-description (s/keys :req-un [::template]
                                     :opt-un [::id
                                              ::primary-id
                                              ::persistable
                                              ::traits
                                              ::transients
                                              ::before-build
                                              ::after-build]))

(s/def :build/with ::template)
(s/def :build/traits (s/coll-of keyword?))
(s/def :build/without (s/coll-of keyword?))
(s/def ::build-opts (s/keys :opt-un [:build/with
                                     :build/traits
                                     :build/without]))

(s/def :output/output-as keyword?)
(s/def :output/transform ifn?)
(s/def :output/persist-with keyword?)
(s/def ::output-opts (s/keys :opt-un [:output/output-as
                                      :output/transform
                                      :output/persist-with]))
