(ns joe.scratch
  (:require [extrude.directive.interface :as directive]
            [extrude.directive.interface.standard :as std-directives :refer [build build-list]]
            [extrude.entity.interface :as entity]
            [extrude.execution.interface :as execution]
            [extrude.execution-context.interface :as context]
            [extrude.factory.interface :as factory]
            [extrude.directive.interface.standard :as standard]
            [extrude.build-graph.interface :as build-graph]
            [loom.alg :as graph-alg]))

(comment
  (directive/run (standard/constant "a"))
  
  (defmethod directive/run ::foo [_]
    :foo)
  (directive/run (directive/->directive ::foo)))

(def organization
  (factory/->factory
   {:id ::organization
    :primary-id :id
    :template {:id random-uuid
               :name "XCorp"
               :features {:gui true
                          :editing false
                          :undo false}}}))

(def user
  (factory/->factory
   {:id ::user
    :primary-id :id
    :template {:id random-uuid
               :name (std-directives/sequence (partial str "User ") :context)
               :role "user"
               :org (build organization)}
    :traits {:admin {:role "admin"}}}))

(def group
  (factory/->factory
   {:id ::group
    :primary-id :id
    :template {:id random-uuid
               :name "Normies"
               :users (build-list user 3)}}))

(comment
  (factory/factory? user)
  (execution/build user {:with {:name "Bob"}
                         :traits [ :admin]})
  (execution/build user)
  (build-graph/path (execution/build-context user {}) [:org])
  (execution/build group)
  (let [org (execution/build organization)
        org-user (execution/build user {:with {:org org}})
        group (execution/build group
                               {:with {:users (build-list user 3
                                                          {:with {:org org}})}})]
    [org
     org-user
     group])
  (let [org (execution/build organization {})
        users (execution/build-list user 4 {:with {:org org}})]
    (tap> [(meta users) 
           (context/entities-in-build-order (meta users))]))
  )
