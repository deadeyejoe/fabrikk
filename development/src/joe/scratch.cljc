(ns joe.scratch
  (:require [extrude.directive.interface :as directive]
            [extrude.directive.interface.standard :as standard]))

(comment
  (directive/run (standard/constant "a")))
