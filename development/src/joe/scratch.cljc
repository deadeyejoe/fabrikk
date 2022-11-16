(ns joe.scratch
  (:require [extrude.directive.interface :as directive :refer [as]]
            [extrude.directive.interface.standard :as std-directives :refer [build build-list]]
            [extrude.entity.interface :as entity]
            [extrude.execution.interface :as execution]
            [extrude.execution-context.interface :as context]
            [extrude.factory.interface :as factory]
            [extrude.directive.interface.standard :as standard]
            [extrude.build-graph.interface :as build-graph]
            [loom.alg :as graph-alg]
            [extrude.persistence.interface :as persistence]))

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

(def post
  (factory/->factory
   {:id ::post
    :primary-id :id
    :template {:id (std-directives/sequence)
               :title "Fabrikk of society"
               :published false
               :content ""
               :author (build user)}
    :traits {:published {:published true}}}))

(def group
  (factory/->factory
   {:id ::group
    :primary-id :id
    :template {:id random-uuid
               :name "Normies"
               :users (build-list user 3)}}))

(defmethod persistence/persist! :store [entity]
  (persistence/store!
   (-> entity
       (assoc-in [:value :id] (random-uuid))
       (assoc :persisted true))))

(comment
  "Building"
  (factory/factory? user)
  (factory/resolve ::user)

  (execution/build user)
  (execution/build user {:with {:name "Bob"}
                         :traits [ :admin]})
  (let [org (execution/build organization)
        org-user (execution/build user {:with {:org (as :name org)}})]
    [org-user])
  
  (execution/build-list user 2)
  
  (execution/build post)

(let [[u1 u2 u3] (execution/build-list user 3)]
  (execution/build post {:with {:author u1}}))

  (build-graph/path (execution/build-context user {}) [:org])
  (execution/build group)
  (let [org (execution/build organization)
        org-user (execution/build user {:with {:org org}})
        group (execution/build group
                               {:with {:users (build-list user 3
                                                          {:with {:org org}})}})]
    [org
     org-user
     group
     (tap> ( meta group))])
  )
(comment
  "Creation"
  (let [org (execution/build organization {})
        users (execution/build-list user 4 {:with {:org org}})]
    (tap> [(meta users)
           (context/entities-in-build-order (meta users))]))

  (do
    (persistence/reset-store!)
    (let [user (execution/create user)]
      [user
       @persistence/store]))

  (do
    (persistence/reset-store!)
    (let [org (execution/build organization)
          users (execution/create-list user 2 {:with {:org org}})]
      [users
       @persistence/store]))

(do
  (persistence/reset-store!)
  (let [org (execution/build organization)
        users (execution/build-list user 2 {:with {:org org}})
        group (execution/create group {:with {:users users}})]
    [group
     @persistence/store]))
  )
  
