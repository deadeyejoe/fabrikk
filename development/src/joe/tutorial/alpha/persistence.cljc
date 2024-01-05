(ns joe.tutorial.alpha.persistence
  (:require [fabrikk.alpha.core :as fab]))

(defn admin-email []
  (str "admin-" (rand-int 10000) "@example.com"))

(def user
  (fab/->factory ::user
   {
    :primary-id :id
    :template {:id (fab/sequence)
               :name "John Smith"
               :email "john@example.org"
               :role "user"
               :verified true}
    :traits {:admin {:name (fab/derive :id (partial str "Admin-"))
                     :email admin-email
                     :role "admin"}
             :unverified {:verified false}}}))

(def post
  (fab/->factory ::post
                 {:template {:id random-uuid
                             :title "This one weird trick"
                             :content "Some content goes here...."
                             :author (fab/one ::user)
                             :author-name (fab/derive [:author] :name)}}))

(defonce my-store (atom {}))
(reset! my-store {})
(def collect (fnil conj []))

(defmethod fab/persist! :my-store [factory-id entity]
  (let [persisted (assoc entity :id (case factory-id
                                      ::user (rand-int 1000000)
                                      ::post (random-uuid)))]
    (swap! my-store update factory-id collect persisted)
    persisted))
(fab/set-default-persistence :my-store)

(fab/create post)

@my-store
;; => {:user
;;     [{:id 846761,
;;       :name "John Smith",
;;       :email "john@example.org",
;;       :role "user",
;;       :verified true}],
;;     :post
;;     [{:author 846761,
;;       :id #uuid "e9382f32-32b4-47fb-8c6c-089ca0a6a025",
;;       :title "This one weird trick",
;;       :content "Some content goes here....",
;;       :author-name "John Smith"}]}


(reset! my-store {})
(let [author (fab/build user)]
  (fab/create-list post 2 {:with {:author author}}))

(defmethod fab/persist! :my-store [factory-id entity]
  (let [persisted (case factory-id
                    ::user (assoc entity
                                  :id (rand-int 10000)
                                  :name (str "User-" (rand-int 10000)))
                    ::post (assoc entity :id (random-uuid)))]
    (swap! my-store update factory-id collect persisted)
    persisted))

(fab/create post)
;; => {:author 4416,
;;     :id #uuid "92fd0318-19e7-4e4a-bee9-d555c99da9e3",
;;     :title "This one weird trick",
;;     :content "Some content goes here....",
;;     :author-name "User-5570"}
