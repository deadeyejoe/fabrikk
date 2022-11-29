(ns fabrikk.alpha.core-test
  (:require [clojure.test :as test :refer :all]
            [fabrikk.alpha.core :as fab]
            [fabrikk.directives.interface :as directives]
            [fabrikk.utils.interface :as utils]
            [fabrikk.execution-context.interface :as context]))

(defn fixture [f]
  (directives/reset-sequence!)
  (f))

(use-fixtures :each fixture)

(def user
  (fab/->factory
   {:id ::user
    :template {:id (fab/sequence)
               :name "Joe"
               :role "reader"}
    :traits {:admin {:role "admin"}}}))

(def post
  (fab/->factory
   {:id ::post
    :primary-id :id
    :template {:id (fab/sequence)
               :title "How to test fabrikk"
               :author (fab/one user)}}))

(defn coerce-to-list [x+]
  (if (sequential? x+) x+ [x+]))

(defn repeat-if-single [x+]
  (if (sequential? x+) x+ (repeat x+)))

(defn ->value-fn [value-descriptor]
  (cond
    (sequential? value-descriptor) #(get-in % (vec value-descriptor))
    (fn? value-descriptor)  value-descriptor
    (keyword? value-descriptor) #(get % value-descriptor)))

(defn match [value+ value-descriptor entity+]
  (let [value-fn (->value-fn value-descriptor)]
    (->> (coerce-to-list entity+)
         (map value-fn)
         (map vector (repeat-if-single value+))
         (every? (partial apply =)))))

(comment
  [(match 1 :k {:k 1})
   (match 1 :k {:k 2})
   (match 1 [:k :k2] {:k {:k2 1}})
   (match 1 [:k :k2] {:k {:k2 2}})
   (match 1 :k [{:k 1} {:k 1}])
   (match 1 :k [{:k 1} {:k 2}])
   (match 1 [:k :k] [{:k {:k 1}} {:k {:k 1}}])
   (match 1 [:k :k] [{:k {:k 1}} {:k {:k 2}}])
   (match [1 2] [:k :k] [{:k {:k 1}} {:k {:k 2}}])
   (match [1 1] [:k :k] [{:k {:k 1}} {:k {:k 2}}])])

(deftest build-single
  (is (= {:id 1
          :name "Joe"
          :role "reader"}
         (fab/build user)))
  (is (match "John" :name (fab/build user {:with {:name "John"}})))
  (is (match "Jane" :name (fab/build user {:with {:name #(str "Ja" "ne")}})))
  (is (match "admin" :role (fab/build user {:traits [:admin]})))
  (is (match nil :name (fab/build user {:without [:name]})))
  (is (match 1 :extra (fab/build user {:with {:extra 1}})))
  (is (match "Joe" :name (fab/build user {:as :name})) "as has no effect at top level"))

(deftest build-list
  (is (= [{:id 1
           :name "Joe"
           :role "reader"}] (fab/build-list user 1)))
  (is (match "John" :name (fab/build-list user 10 {:with {:name "John"}})))
  (is (match "admin" :role (fab/build-list user 10 {:traits [:admin]})))
  (is (match nil :name (fab/build-list user 10 {:without [:name]})))
  (is (match 1 :extra (fab/build-list user 10 {:with {:extra 1}})))
  (let [[jane & rest] (fab/build-list user 3 [{:with {:name "Jane"}}
                                              {:with {:name "John"}}])]
    (is (match "Jane" :name jane))
    (is (match "John" :name rest)))
  (is (context/meta-result? (fab/build-list post 10)) "top level list is has a meta context")
  (is (every? context/meta-result? (fab/build-list post 10)) "suppresses primary id at top level")
  (is (every? #{"Joe"} (fab/build-list user 10 {:as :name})) "'as' works at top-level"))

(deftest constant
  (is (= identity (get (fab/build user {:with {:const (fab/constant identity)}}) :const))))

(deftest sequence
  (is (match (range 1 11) :id (fab/build-list user 10)))
  (is (match (map str (range 11 21)) :id
             (fab/build-list user 10 {:with {:id (fab/sequence str)}})))
  (is (match (range 1 11) :id
             (fab/build-list user 10 {:with {:id (fab/sequence identity :foo)}}))))

(deftest one
  (is (match "Joe" [:author :name] (fab/build post)))
  (is (match "Joe" [:name] (first (fab/build post
                                             {:with {:author (fab/one user {:as :id})}}
                                             {:output-as :build-order}))))
  (is (match "Joe" :author (fab/build post {:with {:author (fab/one user {:as :name})}})))
  (is (match "Joe" [:author :name] (fab/build-list post 10)))
  (is (->> (fab/build-list post 10)
           (map :author)
           (apply distinct?)) "Does not unify by default"))

(deftest many
  (let [{:keys [editors] :as post-with-editors} (fab/build post {:with {:editors (fab/many user 3)}})]
    (is (= 3 (count editors)))
    (is (match "Joe" :name editors)))
  (is (match ["Jane" "Ernest" "Ernest"] :name
             (:editors (fab/build post
                                  {:with {:editors (fab/many user
                                                             3 [{:with {:name "Jane"}}
                                                                {:with {:name "Ernest"}}])}}))))
  (is (every? #{"Ernest"} (:editors (fab/build post
                                               {:with {:editors (fab/many user
                                                                          3 {:with {:name "Ernest"}
                                                                             :as :name})}})))))

(def topic
  (fab/->factory
   {:id ::topic
    :primary-id :id
    :template {:name "Testing"
               :tags ["stuff" "things"]
               :posts (fab/many post 2)
               :moderator (fab/one user)}}))

(deftest incremental-build
  (let [author (fab/build user {:as :id})
        posts (fab/build-list post 2 {:with {:author author}})
        {:keys [post topic user] :as output} (fab/build topic {:with {:posts posts
                                                                      :moderator author}}
                                                        {:output-as :collection})]
    ;; posts are output as entities, but need to be assoc'ed as their primary id..
    ;; maybe lists should control the assoc behaviour of their members?
    (is (= 2 (count post)))
    (is (= 1 (count user)))))

(deftest derive)
