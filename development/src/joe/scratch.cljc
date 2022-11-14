(ns joe.scratch
  (:require [extrude.directive.interface :as directive]
            [extrude.directive.interface.standard :refer [build build-list]]
            [extrude.entity.interface :as entity]
            [extrude.execution.interface :as execution]
            [extrude.execution-context.interface :as context]
            [extrude.factory.interface :as factory]
            [extrude.directive.interface.standard :as standard]
            [extrude.build-graph.interface :as build-graph]))

(comment
  (directive/run (standard/constant "a"))
  
  (defmethod directive/run ::foo [_]
    :foo)
  (directive/run (directive/->directive ::foo)))

(def organization
  (factory/->factory
   {:factory-id ::organization
    :primary-id :id
    :template {:id random-uuid
               :name "XCorp"
               :features {:gui true
                          :editing false
                          :undo false}}}))

(def user
  (factory/->factory
   {:factory-id ::user
    :primary-id :id
    :template {:id random-uuid
               :name "Joe"
               :role "user"
               :org (build organization)}
    :traits {:admin {:role "admin"}}}))

(def group
  (factory/->factory
   {:factory-id ::group
    :primary-id :id
    :template {:id random-uuid
               :name "Normies"
               :users (build-list user 3)}}))

(comment
  (execution/build user {:with {:name "Bob"}
                         :traits [ :admin]})
  (execution/build user {})
  (build-graph/path (execution/build-context user {}) [:org])
  (execution/build group)
  )
