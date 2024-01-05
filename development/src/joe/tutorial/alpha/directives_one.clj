(ns joe.tutorial.alpha.directives-one
  (:require [fabrikk.alpha.core :as fab]))

(defn admin-email []
  (str "admin-" (rand-int 10000) "@example.com"))

(def user
  (fab/->factory ::user
                 {:template {:id (fab/sequence)
                             :name "John Smith"
                             :email "john@example.org"
                             :role "user"
                             :verified true}
                  :traits {:admin {:email admin-email
                                   :role "admin"}
                           :unverified {:verified false}}}))

(fab/build user)
;; => {:id 1, :name "John Smith", :email "john@example.org", :role "user", :verified true}

(fab/build user)
;; => {:id 2, :name "John Smith", :email "john@example.org", :role "user", :verified true}

(fab/build-list user 2 {:traits [:admin]})
;; => [{:id 3, :name "John Smith", :email "admin-9325@example.com", :role "admin", :verified true}
;;     {:id 4, :name "John Smith", :email "admin-1038@example.com", :role "admin", :verified true}]
