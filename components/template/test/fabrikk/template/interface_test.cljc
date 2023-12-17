(ns fabrikk.template.interface-test
  (:require [clojure.test :as test :refer :all]
            [fabrikk.template.interface :as template]
            [fabrikk.template.core :as core])) 

(def simple-template
  (template/compile [[[:name "a"]
                      [:role "admin"]]]))

(deftest test-exists? 
  (is (template/exists? simple-template :name))
  (is (template/new? simple-template :email)))

(deftest test-insert
  (is (= [:name :role :email]
         (-> simple-template
             (template/insert :email "admin@example.com")
             ::core/ordering)))
  (is (= simple-template (template/insert simple-template :name "b"))))

(deftest test-update-existing
  (is (= simple-template (template/update-existing simple-template :email "admin@example.com")))
  (is (= "b" (-> simple-template
                 (template/update-existing :name "b")
                 (get-in [::core/field->tuple :name 1])))))

(deftest test-upsert
  (is (= [:name :role :email]
         (-> simple-template
             (template/upsert :email "admin@example.com")
             ::core/ordering)))
  (is (= "b" (-> simple-template
                 (template/upsert :name "b")
                 (get-in [::core/field->tuple :name 1])))))

(deftest test-without
  (is (= (template/compile [[[:name "a"]]])
         (template/without simple-template [:role :email]))))
