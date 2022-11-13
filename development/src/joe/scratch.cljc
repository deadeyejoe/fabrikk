(ns joe.scratch
  (:require [extrude.directive.interface :as directive]
            [extrude.entity.interface :as entity]
[extrude.execution.interface :as execution]
[extrude.execution-context.interface :as context]
[extrude.factory.interface :as factory]
            [extrude.directive.interface.standard :as standard]))

(comment
  (directive/run (standard/constant "a"))
  
  (defmethod directive/run ::foo [_]
    :foo)
  (directive/run (directive/->directive ::foo)))

(def user
  (factory/->factory
   {:factory-id ::user
    :primary-id :id
    :template {:id random-uuid
               :name "Joe"
               :role "user"}
    :traits {:admin {:role "admin"}}}))

(comment
  (-> (execution/build user {})
      (context/->result-meta)
      (meta)))
