(ns fabrikk.template.interface.specs 
  (:require [clojure.spec.alpha :as s]))


(s/def ::field keyword?)
(s/def ::value any?)
(s/def ::template-map (s/map-of ::field ::value))
(s/def ::template-tuple (s/cat :field ::field :value ::value))
(s/def ::fragment (s/or :map ::template-map
                        :tuple ::template-tuple))
(s/def ::fragment-list (s/coll-of ::fragment :kind sequential?))
(s/def ::description (s/or :map ::template-map
                           :list ::fragment-list))

(s/def ::ordering (s/coll-of ::field :kind sequential?))
(s/def ::field->tuple (s/map-of ::field ::template-tuple))
(s/def ::compiled (s/keys :req-un [::ordering
                                   ::field->tuple]))
