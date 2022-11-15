(ns extrude.entity.interface-test
  (:require [extrude.entity.interface :as entity]
            [clojure.test :as test :refer :all]))

(deftest reflexive
  (let [instance (entity/create! {} {1 2})
        persisted (-> (entity/create! {} {1 2})
                      (assoc :persisted true))]
    (is (entity/no-conflict? instance instance))
    (is (= instance (entity/combine instance instance)))
    (is (entity/no-conflict? persisted persisted))
    (is (= persisted (entity/combine persisted persisted)))))

(deftest uuid-mismatch
  (let [one (entity/create! {} {1 2})
        two (entity/create! {} {1 2})]
    (is (entity/no-conflict? one two))
    (is (nil? (entity/combine one two)))))

(deftest uuid-match
  (testing "value mismatch"
    (testing "and not persisted"
      (let [one (entity/create! {} {1 2})
            two (assoc one :value {3 4})]
        (is (entity/conflict? one two))
        (is (nil? (entity/combine one two)))))
    (testing "and persisted"
      (let [one (-> (entity/create! {} {1 2})
                    (assoc :persisted true))
            two (assoc one :value {3 4})]
        (is (entity/conflict? one two))
        (is (nil? (entity/combine one two))))))
  (testing "persisted mismatch"
    (let [one (entity/create! {} {1 2})
          two (assoc one :persisted true)]
      (is (entity/no-conflict? one two))
      (is (= two (entity/combine one two)))
      (is (= two (entity/combine two one))))))
