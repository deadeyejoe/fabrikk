(ns extrude.factory.core
  (:require [extrude.specs.interface :as specs]
            [clojure.spec.alpha :as s]))

(defn ->factory [factory] factory)
(s/fdef ->factory
  :ret ::specs/factory)
