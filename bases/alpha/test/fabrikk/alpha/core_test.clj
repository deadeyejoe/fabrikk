(ns fabrikk.alpha.core-test
  (:require [clojure.test :as test :refer :all]
            [fabrikk.alpha.core :as fab]
            [fabrikk.directives.interface :as directives]
            [fabrikk.utils.interface :as utils]
            [fabrikk.execution-context.interface :as context]
            [fabrikk.factory.interface :as factory]
            [medley.core :as medley])
  (:refer-clojure :exclude [type]))

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
    :traits {:admin {:role "admin"}
             :locked {:locked true}
             :moderator {:role "moderator"
                         :privileges "cru"}}}))

(def post
  (fab/->factory
   {:id ::post
    :primary-id :id
    :template {:id (fab/sequence)
               :title "How to test fabrikk"
               :author (fab/one user)}}))

(deftest test-build-single
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

(deftest test-traits
  (is (match "admin" :role (fab/build user {:traits [:admin]})))
  (is (match "moderator" :role (fab/build user {:traits [:moderator]})))
  (testing "Composing"
    (let [locked-admin (fab/build user {:traits [:locked :admin]})
          locked-moderator (fab/build user {:traits [:locked :moderator]})]
      (is (match ["admin" "moderator"] :role [locked-admin locked-moderator]))
      (is (match true :locked [locked-admin locked-moderator]))))
  (testing "Ordering"
    (let [admin-first (fab/build user {:traits [:admin :moderator]})
          moderator-first (fab/build user {:traits [:moderator :admin]})]
      (is (match ["moderator" "admin"] :role [admin-first moderator-first]))
      (is (match "cru" :privileges [admin-first moderator-first])))))

(deftest test-build-list
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
  (is (every? context/meta-result? (fab/build-list post 10)) "elements have a meta context")
  (is (every? context/meta-result? (fab/build-list user 10 {:as :name})) "'as' does not change output"))

(deftest test-constant
  (is (= identity (get (fab/build user {:with {:const (fab/constant identity)}}) :const))))

(deftest test-sequence
  (is (match (range 1 11) :id (fab/build-list user 10)))
  (is (match (map str (range 11 21)) :id
             (fab/build-list user 10 {:with {:id (fab/sequence str)}})))
  (is (match (range 1 11) :id
             (fab/build-list user 10 {:with {:id (fab/sequence identity :foo)}}))))

(deftest test-one
  (is (match "Joe" [:author :name] (fab/build post)))
  (is (match "Joe" [:name] (first (fab/build post
                                             {:with {:author (fab/one user {:as :id})}}
                                             {:output-as :build-order}))))
  (is (match "Joe" :author (fab/build post {:with {:author (fab/one user {:as :name})}})))
  (is (match "Joe" [:author :name] (fab/build-list post 10)))
  (is (->> (fab/build-list post 10)
           (map :author)
           (apply distinct?)) "Does not unify by default"))

(deftest test-many
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
                                                                             :as :name})}})))
      "'as' changes association value"))

(def topic
  (fab/->factory
   {:id ::topic
    :primary-id :id
    :template {:name "Testing"
               :tags ["stuff" "things"]
               :posts (fab/many post 2)
               :moderator (fab/one user)}}))

(deftest test-incremental-build
  (let [author (fab/build user {:as :id})
        posts (fab/build-list post 2 {:with {:author author}})
        {:keys [post user]
         [topic] :topic :as output} (fab/build topic {:with {:posts posts
                                                             :moderator author}}
                                               {:output-as :collection})]
    (is (= 2 (count post)))
    (is (= 1 (count user)))
    (is (match (:id author) :author posts))
    (is (= (set (map :id posts)) (set (:posts topic))))))

(def type
  (fab/->factory
   {:id ::type
    :template {:id (random-uuid)
               :name "Thing"}}))
(def model
  (fab/->factory
   {:id ::model
    :primary-id :id
    :template [{:id (fab/sequence)
                :name "model"}
               [:node-types (fab/many type 1 {:with {:name "Person"}})]
               [:edge-types (fab/many type 1 {:with {:name "Knows"}})]]
    :traits {:org-structure {:name "Org Structure"
                             :node-types (fab/many type 4 [{:with {:name "Division"}}
                                                           {:with {:name "Department"}}
                                                           {:with {:name "Team"}}
                                                           {:with {:name "Person"}}])
                             :edge-types (fab/many type 2 [{:with {:name "Belongs to"}}
                                                           {:with {:name "Reports to"}}])}}
    :after-build (fn [ctx value]
                   (-> value
                       (update :node-types (partial medley/index-by :id))
                       (update :edge-types (partial medley/index-by :id))))}))
(def node
  (fab/->factory
   {:id ::node
    :primary-id :id
    :template {:id (fab/sequence)
               :model (fab/one model)
               :type (fab/derive [:model :node-types 0] :name)
               :name "John O'Reilly"}}))

(deftest test-after-build
  (let [{:keys [node-types edge-types] :as model} (fab/build model {:traits [:org-structure]})]
    (is (map? node-types))
    (is (map? edge-types))))

(deftest test-derive
  (let [{[person-type knows-type] :type
         [built-node] :node
         [model] :model
         :as coll} (fab/build node {} {:output-as :collection})
        _ (tap> coll)
        with-type-id (fab/build node {:with {:type-id (fab/derive [:model :node-types 0] :id)}})]
    (is (match "Person" :type built-node))
    (is (match (:id model) :model built-node))
    (is (match (:id person-type) :type-id with-type-id))
    (testing "Derive self and function"
     (is (match "Type: Person" :type-label (fab/build node
                                                      {:with {:type-label (fab/derive :type
                                                                                      (partial str "Type: "))}})))))
  (let [org-model (fab/build model {:traits [:org-structure]})]
    (is (match "Division" :type (fab/build node {:with {:name "EMEA" :model org-model}})))
    (is (match "Department" :type (fab/build node {:with {:name "Accounting"
                                                          :model org-model
                                                          :type (fab/derive [:model :node-types 1] :name)}})))))

(def do-persist! identity)
(defmethod fab/persist! :store [entity]
  (do-persist! entity))

(deftest test-create
  (with-redefs [do-persist! #(assoc %
                                    :id (random-uuid)
                                    :persisted true)]
    (testing "create single"
      (let [random-id (random-uuid)
            built (fab/build user {:with {:id random-id}})
            created (fab/create user {:with {:id random-id}})]
        (is (:persisted created))
        (is (= random-id (:id built)))
        (is (not= random-id (:id created)))))
    (testing "create with sub resource"
      (let [[created-user post] (fab/create post {} {:output-as :build-order})]
        (is (match true :persisted [created-user post]))))
    (testing "sub-resource referenced as identity"
      (let [built-user (fab/build user)
            [created-user post] (fab/create post {:with {:author built-user}} {:output-as :build-order})]
        (is (match true :persisted [created-user post]))
        (is (not= (:id built-user) (:id created-user)))
        (is (= (get-in post [:author :id]) (:id created-user)))))
    (testing "sub-resource referenced as id"
      (let [built-user (fab/build user {:as :id})
            [created-user post] (fab/create post {:with {:author built-user}} {:output-as :build-order})]
        (is (match true :persisted [created-user post]))
        (is (not= (:id built-user) (:id created-user)))
        (is (= (:author post) (:id created-user)))))
    (testing "derived properties and unification and more"
      (let [built-user (fab/build user {:as :id})
            built-posts (fab/build-list post 2
                                        {:with {:author built-user
                                                :status (fab/derive [:author]
                                                                    #(if (:persisted %) "Saved" "Unsaved"))}})
            {[created-user & other-users] :user
             posts :post
             [topic] :topic} (fab/create topic
                                         {:with {:posts built-posts
                                                 :moderator built-user
                                                 :moderator-label (fab/derive [:moderator]
                                                                              #(-> % :name ((partial str "Mod: "))))}}
                                         {:output-as :collection})]
        (is (empty? other-users))
        (is (match "Unsaved" :status built-posts))
        (is (match "Saved" :status posts))
        (is (= (:id created-user) (:moderator topic)))
        (is (= (str "Mod: " (:name built-user)) (:moderator-label topic)))))))

(deftest test-create-list
  (let [call-count (atom 0)]
    (with-redefs [do-persist! (fn [entity]
                                (swap! call-count inc)
                                (assoc entity
                                       :id (random-uuid)
                                       :persisted true))]
      (let [built-user (fab/build user {:as :id})
            [created-user & created-posts] (fab/create-list post 3
                                                            {:with {:author built-user}}
                                                            {:output-as :build-order})]
        (is (= 4 @call-count))
        (is (match (:id created-user) :author created-posts)))))
  (is (context/meta-result? (fab/create-list post 10)) "Result is a meta context")
  (is (every? context/meta-result? (fab/create-list post 10)) "Elements are meta contexts")
  (is (every? context/meta-result? (fab/create-list user 10 {:as :name})) "'as' does not change output"))
