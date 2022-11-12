(ns joe.scratch
  (:require [extrude.directive.interface :as directive]
            [extrude.directive.interface.standard :as standard]))

(comment
  (directive/run (standard/constant "a"))
  
  (defmethod directive/run ::foo [_]
    :foo)
  (directive/run (directive/->directive ::foo)))
