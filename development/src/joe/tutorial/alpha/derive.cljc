(ns joe.tutorial.alpha.derive
  (:require [fabrikk.alpha.core :as fab]))

(defn admin-email []
  (str "admin-" (rand-int 10000) "@example.com"))

(def user
  (fab/->factory ::user
                 {:primary-id :id
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


(fab/build user {:traits [:admin]})
;; => {:id 1, :name "Admin-1", :email "admin-8575@example.com", :role "admin", :verified true}
(fab/build user {:with {:id 100}
                 :traits [:admin]})
;; => {:id 100, :name "Admin-100", :email "admin-8902@example.com", :role "admin", :verified true}
(fab/build user {:with {:name "Omega-Admin"}
                 :traits [:admin]})
;; => {:id 2, :name "Omega-Admin", :email "admin-7767@example.com", :role "admin", :verified true}

(fab/build post)
;; => {:author 3,
;;     :id #uuid "233c7e97-f7cf-413b-89e3-69b782c9a4d8",
;;     :title "This one weird trick",
;;     :content "Some content goes here....",
;;     :author-name "John Smith"}

(let [jimmy (fab/build user {:with {:name "Jimmy Murphy"}})]
  (fab/build post {:with {:author jimmy}}))
;; => {:author 6,
;;     :id #uuid "d291eb5b-9e68-460d-ad35-cfea8073d485",
;;     :title "This one weird trick",
;;     :content "Some content goes here....",
;;     :author-name "Jimmy Murphy"}

(def post
  (fab/->factory ::post
                 {:template {:id random-uuid
                             :title "This one weird trick"
                             :content "Some content goes here...."
                             :author (fab/one ::user)
                             :author-name (fab/derive :author str)}}))

(fab/build post)
;; => {:author 8,
;;     :id #uuid "49910b5c-a0d4-41b9-b232-e910fcb835c4",
;;     :title "This one weird trick",
;;     :content "Some content goes here....",
;;     :author-name "8"}
