(ns fabrikk.entity.interface-test
  (:require [fabrikk.entity.interface :as entity]
            [clojure.test :as test :refer :all]))

(deftest combine-reflexive
  (let [instance (entity/create! {} {1 2})
        persisted (-> (entity/create! {} {1 2})
                      (assoc :persisted true))]
    (is (entity/no-conflict? instance instance))
    (is (= instance (entity/combine instance instance)))
    (is (entity/no-conflict? persisted persisted))
    (is (= persisted (entity/combine persisted persisted)))))

(deftest combine-uuid-mismatch
  (let [one (entity/create! {} {1 2})
        two (entity/create! {} {1 2})]
    (is (entity/no-conflict? one two))
    (is (nil? (entity/combine one two)))))

(deftest combine-uuid-match
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

(deftest combine-nil
  (let [one (entity/create! {} {1 2})]
    (is (entity/no-conflict? one nil))
    (is (entity/no-conflict? nil one))
    (is (= one (entity/combine one nil)))
    (is (= one (entity/combine nil one)))))

(deftest test-associate-as
  (let [with-no-factory (entity/create! {} {})
        with-factory (entity/create! {:primary-key ::factory} {})
        with-build-opt (entity/create! {:primary-key ::factory} {:as ::build-opt})]
    (testing "Identity is the fallback"
      (is (= :identity (entity/associate-as (entity/create-list!))))
      (is (= :identity (entity/associate-as with-no-factory))))
    (testing "Followed by factory primary id"
      (is (= ::factory (entity/associate-as with-factory))))
    (testing "Followed by build-opts"
      (is (= ::build-opt (entity/associate-as with-build-opt))))
    (testing "Followed by directive override"
      (is (= ::override (entity/associate-as (entity/override-association with-no-factory ::override))))
      (is (= ::override (entity/associate-as (entity/override-association with-factory ::override))))
      (is (= ::override (entity/associate-as (entity/override-association with-build-opt ::override)))))))

(deftest test-value-to-assoc
  (let [entity (-> (entity/create! {:primary-key ::factory} {})
                   (entity/update-value assoc :id 1))]
    (is (= {:id 1} (entity/value-to-assoc entity :identity)))
    (is (= {:id 1} (entity/value-to-assoc entity :itself)))
    (is (= {:id 1} (entity/value-to-assoc entity :list-item)))
    (is (= {:id 1} (entity/value-to-assoc entity [])))
    (is (= {:id 1} (entity/value-to-assoc entity nil)))
    (is (= 1 (entity/value-to-assoc entity :id)))
    (is (= "1" (entity/value-to-assoc entity (comp str :id))))))

(deftest test-suppress-list-association
  (let [no-build-opt (entity/create! {:primary-key ::factory} {})
        other-build-opt (entity/create! {:primary-key ::factory} {:as ::something})
        list-build-opt (entity/create! {:primary-key ::factory} {:as entity/list-item-kw})]
    (is (nil? (-> no-build-opt (entity/suppress-list-association) :build-opts :as)))
    (is (= ::something (-> other-build-opt (entity/suppress-list-association) :build-opts :as)))
    (is (nil? (-> list-build-opt (entity/suppress-list-association) :build-opts :as)))))
