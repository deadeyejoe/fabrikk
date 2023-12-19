(ns fabrikk.template.interface-test
  (:require [clojure.test :as test :refer :all]
            [fabrikk.template.interface :as template])) 

(def simple-template
  (template/compile [[:name "a"]
                     [:role "admin"]]))

(deftest test-merge
  (is (= simple-template
         (template/merge simple-template (template/compile [[:role "admin"]
                                                            [:name "a"]])))
      "Ordering is maintained")
  (let [to-merge (template/compile [[:role "user"]
                                    [:email "admin@example.com"]])
        {:keys [ordering] :as merged} (template/merge simple-template to-merge)]
    (is (= (last ordering) :email)
        "New fields are added to the end of the ordering")
    (is (= (template/value merged :role) "user")
        "Existing fields are overwritten")))

(deftest test-exists? 
  (is (template/exists? simple-template :name))
  (is (template/new? simple-template :email)))

(deftest test-insert
  (is (= [:name :role :email]
         (-> simple-template
             (template/insert :email "admin@example.com")
             :ordering)))
  (is (= simple-template (template/insert simple-template :name "b"))))

(deftest test-update-existing
  (is (= simple-template (template/update-existing simple-template :email "admin@example.com")))
  (is (= "b" (-> simple-template
                 (template/update-existing :name "b")
                 (template/value :name)))))

(deftest test-upsert
  (is (= [:name :role :email]
         (-> simple-template
             (template/upsert :email "admin@example.com")
             :ordering)))
  (is (= "b" (-> simple-template
                 (template/upsert :name "b")
                 (template/value :name)))))

(deftest test-without
  (is (= (template/compile [[:name "a"]])
         (template/without simple-template [:role :email]))))
