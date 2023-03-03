(ns joe.tutorial.alpha.build
  (:require [fabrikk.alpha.core :as fab]))

(def user
  (fab/->factory
   {:id ::user
    :template {:name "John Smith"
               :email "john@example.org"
               :role "admin"}}))

(fab/build user)
;; we use with to change the name:
(fab/build user {:with {:name "John Murphy"}})
;; => {:name "John Murphy", :email "john@example.org", :role "admin"}
;; or add an arbitrary key to the map:
(fab/build user {:with {:favourite-food "chips"}})
;; => {:name "John Smith", :email "john@example.org", :role "admin", :favourite-food "chips"}
