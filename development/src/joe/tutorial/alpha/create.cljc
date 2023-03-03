(ns joe.tutorial.alpha.create
  (:require [fabrikk.alpha.core :as fab]))

(defn admin-email []
  (str "admin-" (rand-int 10000) "@example.com"))

(def user
  (fab/->factory
   {:id ::user
    :primary-id :id
    :template {:id (fab/sequence)
               :name "John Smith"
               :email "john@example.org"
               :role "user"
               :verified true}
    :traits {:admin {:email admin-email
                     :role "admin"}
             :unverified {:verified false}}}))

(def post
  (fab/->factory
   {:id ::post
    :template {:id random-uuid
               :title "This one weird trick"
               :content "Some content goes here...."
               :author (fab/one ::user)}}))
(fab/set-default-persistence :store)

(fab/create post)

@fab/store
(fab/reset-store!)

(let [author (fab/build user)]
  (fab/create-list post 2 {:with {:author author}}))

(defonce my-store (atom {}))
(reset! my-store {})
(def collect (fnil conj []))

(defmethod fab/persist! :my-store [factory-id entity]
  (let [persisted (assoc entity :id (case factory-id
                                      ::user (rand-int 1000000)
                                      ::post (random-uuid)))]
    (swap! my-store update factory-id collect persisted)
    (doto persisted (tap>))))
(fab/set-default-persistence :my-store)

(fab/create post)

@my-store

(reset! my-store {})
(let [author (fab/build user)]
  (fab/create-list post 2 {:with {:author author}}))
