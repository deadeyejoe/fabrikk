(ns joe.tutorial.alpha.referencing
  (:require [fabrikk.alpha.core :as fab]))

(defn admin-email []
  (str "admin-" (rand-int 10000) "@example.com"))

(def user
  (fab/->factory
   {:id ::user
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

(fab/build post)
;; => {:author {:id 2, :name "John Smith", :email "john@example.org", :role "user", :verified true},
;;     :id #uuid "afe9eb8d-4722-4718-ab42-5fc50d766473",
;;     :title "This one weird trick",
;;     :content "Some content goes here...."}

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

(fab/build post)
;; => {:author 4,
;;     :id #uuid "67a43dea-f992-47f7-a78a-761a29de9f1c",
;;     :title "This one weird trick",
;;     :content "Some content goes here...."}

(fab/build post {} {:output-as :context})
;; => ({:id 5, :name "John Smith", :email "john@example.org", :role "user", :verified true}
;;     {:author 5,
;;      :id #uuid "83ee842e-ee37-4dd1-b385-6aba8f7a6c0f",
;;      :title "This one weird trick",
;;      :content "Some content goes here...."})

(fab/build-list post 2)
;; => [{:author 6,
;;      :id #uuid "8f3b46d2-2efd-4689-82c2-23f62db30886",
;;      :title "This one weird trick",
;;      :content "Some content goes here...."}
;;     {:author 7,
;;      :id #uuid "36c0a332-b878-4527-a9df-3e6460c455bf",
;;      :title "This one weird trick",
;;      :content "Some content goes here...."}]

(let [author (fab/build user)]
  (fab/build-list post 2
                  {:with {:author author}}
                  {:output-as :build-order}))
;; => ({:id 9, :name "John Smith", :email "john@example.org", :role "user", :verified true}
;;     {:author 9,
;;      :id #uuid "593186f2-254d-4a4a-acde-f8a2595e00bc",
;;      :title "This one weird trick",
;;      :content "Some content goes here...."}
;;     {:author 9,
;;      :id #uuid "fb0d88e9-fd82-4730-adf3-ddb5b532396f",
;;      :title "This one weird trick",
;;      :content "Some content goes here...."})

(fab/build post
           {:with {:author "John"}}
           {:output-as :build-order})
;; => ({:author "John",
;;      :id #uuid "1073fbdb-0842-4747-8c46-8bc959d2de8b",
;;      :title "This one weird trick",
;;      :content "Some content goes here...."})


(fab/build post {:with {:editor (fab/one ::user)}})
;; => {:author 8,
;;     :editor 9,
;;     :id #uuid "f99f03e6-270c-4aa1-924a-08606857fe0e",
;;     :title "This one weird trick",
;;     :content "Some content goes here...."}
