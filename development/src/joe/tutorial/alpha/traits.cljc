(ns joe.tutorial.alpha.traits
  (:require [fabrikk.alpha.core :as fab]))

(def user
  (fab/->factory ::user
                 {:template {:name "John Smith"
                             :email "john@example.org"
                             :role "user"
                             :verified true}
                  :traits {:admin {:email "admin-0001@example.org"
                                   :role "admin"}
                           :unverified {:verified false}}}))

(fab/build user)
;; => {:name "John Smith", :email "john@example.org", :role "user", :verified true}

(fab/build user {:traits [:admin]})
;; => {:name "John Smith", :email "admin-0001@example.org", :role "admin", :verified true}

(fab/build user {:traits [:admin :unverified]})
;; => {:name "John Smith", :email "admin-0001@example.org", :role "admin", :verified false}
