(ns joe.scratch
  (:require [fabrikk.directives.interface :as directives :refer [build build-list]]
            [fabrikk.directive-core.interface :as directive-core :refer [associate-as]]
            [fabrikk.factory.interface :as factory]
            [fabrikk.persistence.interface :as persistence]
            [fabrikk.alpha.core :as fab]
            [fabrikk.output.interface :as output]
            [loom.graph :as graph]))

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
    :transients {:foo :bar}
    :after-build (fn [ctx value]
                   (tap> [::after-build ctx value])
                   value)}))

(def post
  (factory/->factory
   {:id ::post
    :primary-id :id
    :template [{:author (fab/one user)}
               [:content-length (fab/derive :content count)]
               {:id (fab/sequence)
                :title "Fabrikk of society"
                :published false
                :content "Content"}]
    :traits {:published {:published true}}}))

(def group
  (factory/->factory
   {:id ::group
    :primary-id :id
    :template {:id random-uuid
               :name "Normies"
               :users (fab/many user 3)}}))



(comment
  "Factory"
  (factory/factory? user)
  (factory/resolve ::user)
  (factory/compile-template post {}))

(comment
  "Derive"
  (fab/build post {:with  {:author-name (fab/derive [:author] :name)}
                   :traits [:published]}
             {:output-as :build-order})
  (let [[post context] (fab/build post {:with  {:author-name (fab/derive [:author] :name)}
                                        :traits [:published]}
                                  {:output-as :tuple})
        prim-node (context/primary context)]
    [(:uuid prim-node)
     (graph/out-edges context (:uuid prim-node))]))

(comment
  "Template"
  (factory/compile-template user {:with {:id :one}}))

(comment
  "Building"

  (fab/build user)
  (fab/build user {:with {:name "Bob"}
                   :traits [:admin]})
  (let [org (fab/build organization)]
    (fab/build user {:with {:org (associate-as org :name)}}))

  (fab/build user)

  (fab/build-list user 2)

  (let [org (fab/build organization)]
    [org
     (fab/build group
                {:with {:users (fab/many user 3
                                         {:with {:org org}})}})]))
(comment
  "Output"
  (fab/build post)
  (fab/build post
             {:with {:author-role (fab/derive [:author] :role)}}
             (output/as-context))
  (fab/build post {} (output/as-collection))
  (fab/build post {} (output/as-tuple))
  (fab/build post {} (output/as-value :content)))

(comment
  "Creation"
  (do
    (persistence/reset-store!)
    (let [user (fab/create user {:persist-with :foo})]
      [user
       @persistence/store]))

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
    (let [post (fab/create post)]
      [post
       @persistence/store])))
