(ns extrude.factory.interface
  (:require [extrude.factory.core :as core]))

(defn ->factory [factory]
  (core/->factory factory))
