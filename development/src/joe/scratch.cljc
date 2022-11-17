(ns joe.scratch
  (:require [fabrikk.directives.interface :as directives :refer [build build-list]]
            [fabrikk.directive-core.interface :as directive-core :refer [as]]
            [fabrikk.entity.interface :as entity]
            [fabrikk.execution.interface :as execution]
            [fabrikk.execution-context.interface :as context]
            [fabrikk.factory.interface :as factory]
            [fabrikk.build-graph.interface :as build-graph]
            [loom.alg :as graph-alg]
            [fabrikk.persistence.interface :as persistence]
            [fabrikk.core :as fab]))

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
               :name (directives/sequence (partial str "User ") :context)
               :role "user"
               :org (fab/one organization)}
    :traits {:admin {:role "admin"}}
    :transients [:foo]
    :after-build (fn [value] 
                   (tap> [::after-build value])
                   value)}))

(def post
  (factory/->factory
   {:id ::post
    :primary-id :id
    :template {:id (fab/sequence)
               :title "Fabrikk of society"
               :published false
               :content ""
               :author (fab/one user)}
    :traits {:published {:published true}}}))

(def group
  (factory/->factory
   {:id ::group
    :primary-id :id
    :template {:id random-uuid
               :name "Normies"
               :users (fab/many user 3)}}))

(defmethod persistence/persist! :store [value]
  (tap> ::store!)
  (persistence/store! (assoc value :id (random-uuid))))

(defmethod persistence/persist! [:foo ::user] [value]
  (tap> ::foo!)
  (persistence/store! (assoc value :id (random-uuid))))

(comment
  "Building"
  (factory/factory? user)
  (factory/resolve ::user)

  (fab/build user {:with {:foo :bar :baz 1}})
  (fab/build user {:with {:name "Bob"}
                         :traits [:admin]})
  (let [org (fab/build organization)
        org-user (fab/build user {:with {:org (as :name org)}})]
    [org-user])

  (fab/build-list user 2)

  (fab/build post)

  (let [[u1 u2 u3] (fab/build-list user 3)]
    (fab/build post {:with {:author u1}}))

  (build-graph/path (execution/build-context user {}) [:org])
  (fab/build group)
  (let [org (fab/build organization)
        org-user (fab/build user {:with {:org org}})
        group (fab/build group
                               {:with {:users (fab/many user 3
                                                          {:with {:org org}})}})]
    [org
     org-user
     group
     (tap> (meta group))])
  )
(comment
  "Creation"
  (let [org (fab/build organization {})
        users (fab/build-list user 4 {:with {:org org}})]
    (tap> [(meta users)
           (context/entities-in-build-order (meta users))]))

  (do
    (persistence/reset-store!)
    (let [user (fab/create user {:persist-with :foo})]
      [user
       @persistence/store]))
  
  (do
    (persistence/reset-store!)
    (let [user (fab/build user)
          context (meta user)
          primary (context/primary context)]
      [(persistence/value-with-dispatch-meta primary {})
       (meta (persistence/value-with-dispatch-meta primary {}))]))

  (do
    (persistence/reset-store!)
    (let [org (fab/build organization)
          users (fab/create-list user 2 {:with {:org org}})]
      [users
       @persistence/store]))

  (do
    (persistence/reset-store!)
    (let [org (fab/build organization)
          users (fab/build-list user 2 {:with {:org org}})
          group (fab/create group {:with {:users users}})]
      [group
       @persistence/store]))
  
  (do
    (persistence/reset-store!)
    (let [org (fab/build organization)
          users (fab/create-list user 2 {:with {:org org}})]
      [users
       @persistence/store]))
)
  
