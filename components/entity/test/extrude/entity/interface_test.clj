(ns extrude.entity.interface-test
  (:require [extrude.entity.interface :as entity]
            [clojure.test :as test :refer :all]))

(deftest primary-id
  (let [entity (entity/build {:factory-id :factory
                              :primary-id :_id}
                             {:_id :foo})]
    (is (= :foo (:id entity)))))

(deftest reflexive
  (let [instance (entity/build {} {1 2})
        persisted (-> (entity/build {} {1 2})
                      (assoc :persisted true))]
    (is (entity/no-conflict? instance instance))
    (is (= instance (entity/combine instance instance)))
    (is (entity/no-conflict? persisted persisted))
    (is (= persisted (entity/combine persisted persisted)))))

(deftest uuid-mismatch
  (let [one (entity/build {} {1 2})
        two (entity/build {} {1 2})]
    (is (entity/no-conflict? one two))
    (is (nil? (entity/combine one two)))))

(deftest uuid-match
  (testing "value mismatch"
    (testing "and not persisted"
      (let [one (entity/build {} {1 2})
            two (assoc one :value {3 4})]
        (is (entity/conflict? one two))
        (is (nil? (entity/combine one two)))))
    (testing "and persisted"
      (let [one (-> (entity/build {} {1 2})
                    (assoc :persisted true))
            two (assoc one :value {3 4})]
        (is (entity/conflict? one two))
        (is (nil? (entity/combine one two))))))
  (testing "persisted mismatch"
    (let [one (entity/build {} {1 2})
          two (assoc one :persisted true)]
      (is (entity/no-conflict? one two))
      (is (= two (entity/combine one two)))
      (is (= two (entity/combine two one))))))
