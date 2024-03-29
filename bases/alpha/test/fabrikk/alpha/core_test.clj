(ns fabrikk.alpha.core-test
  (:require [clojure.test :as test :refer :all]
            [fabrikk.alpha.core :as fab]
            [fabrikk.directives.interface :as directives]
            [fabrikk.output.interface :as output]
            [medley.core :as medley]
            [fabrikk.persistence.interface :as persistence]
            [orchestra.spec.test :as ost])
  (:refer-clojure :exclude [type]))

(ost/instrument)
(persistence/set-default-persistence :store)

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
   ::user
   {:template {:id (fab/sequence)
               :name "Joe"
               :role "reader"}
    :traits {:admin {:role "admin"}
             :locked {:locked true}
             :moderator {:role "moderator"
                         :privileges "cru"}}}))

(def post
  (fab/->factory
   ::post
   {:primary-key :id
    :template {:id (fab/sequence)
               :title "How to test fabrikk"
               :author (fab/one user)}}))

(deftest test-build-single
  (is (= {:id 1
          :name "Joe"
          :role "reader"}
         (fab/build user)))
  (is (= {:id 2
          :name "Joe"
          :role "reader"}
         (user))
      "Calling factory calls build")
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
  (is (output/meta-result? (fab/build-list post 10)) "top level list is has a meta context")
  (is (every? output/meta-result? (fab/build-list post 10)) "elements have a meta context")
  (is (every? output/meta-result? (fab/build-list user 10 {:as :name})) "'as' does not change output"))

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
                                             {:with {:author (fab/one user {:associate-as :id})}}
                                             {:output-as :build-order}))))
  (is (match "Joe" :author (fab/build post {:with {:author (fab/one user {:associate-as :name})}})))
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
                                                                             :associate-as :name})}})))
      "'associate-as' changes association value"))

(deftest test-inherit
  (let [inherited (fab/inherit ::user
                               ::reader-user
                               {:template {:role "reader"
                                           :email "reader@example.com"}
                                :traits {:verified {:verified true}}})]
    (is (not= (:id user) (:id inherited))
        "Inherited factory has a different id")
    (is (= (:id user) (:parent inherited))
        "Inherited factory has correct parent id")
    (is (= ["reader" "reader@example.com"] ((juxt :role :email) (inherited)))
        "template is merged")
    (is (= "admin" (:role (inherited {:traits [:admin]}))))
    (is (:verified (inherited {:traits [:verified]})))))

(def topic
  (fab/->factory
   ::topic
   {:primary-key :id
    :template {:name "Testing"
               :tags ["stuff" "things"]
               :posts (fab/many post 2)
               :moderator (fab/one user)}}))

(deftest test-incremental-build
  (let [author (fab/build user)
        posts (fab/build-list post 2 {:with {:author (fab/associate-as author :id)}})
        {:keys [post user]
         [topic] :topic :as output} (fab/build topic {:with {:posts posts
                                                             :moderator (fab/associate-as author :id)}}
                                               {:output-as :collection})]
    (is (= 2 (count post)))
    (is (= 1 (count user)))
    (is (match (:id author) :author posts))
    (is (= (set (map :id posts)) (set (:posts topic))))))

(def type
  (fab/->factory
   ::type
   {:template {:id (random-uuid)
               :name "Thing"}}))
(def model
  (fab/->factory
   ::model
   {:primary-key :id
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
   ::node
   {:primary-key :id
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

(def do-persist! (fn [_factory-id entity] entity))
(defmethod fab/persist! :store [factory-id entity]
  (do-persist! factory-id entity))

(deftest test-create
  (with-redefs [do-persist! (fn [_factory_id entity]
                              (assoc entity
                                     :id (random-uuid)
                                     :persisted true))]
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
      (let [built-user (fab/build user {:associate-as :id})
            [created-user post] (fab/create post {:with {:author built-user}} {:output-as :build-order})]
        (is (match true :persisted [created-user post]))
        (is (not= (:id built-user) (:id created-user)))
        (is (= (:author post) (:id created-user)))))
    (testing "derived properties and unification and more"
      (let [built-user (fab/build user {:associate-as :id})
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
    (with-redefs [do-persist! (fn [_factory-id entity]
                                (swap! call-count inc)
                                (assoc entity
                                       :id (random-uuid)
                                       :persisted true))]
      (let [built-user (fab/build user {:associate-as :id})
            [created-user & created-posts] (fab/create-list post 3
                                                            {:with {:author built-user}}
                                                            {:output-as :build-order})]
        (is (= 4 @call-count))
        (is (match (:id created-user) :author created-posts)))
      (testing "with meta output"
        (let [built-user (fab/build user {:associate-as :id})
              created-posts (fab/create-list post 3 {:with {:author built-user}})]
          (is (= 3 (count created-posts)))
          (is (not (match (:id built-user) :author created-posts)))))))
  (is (output/meta-result? (fab/create-list post 10)) "Result is a meta context")
  (is (every? output/meta-result? (fab/create-list post 10)) "Elements are meta contexts")
  (is (every? output/meta-result? (fab/create-list user 10 {:as :name})) "'as' does not change output"))

(def do-test-persist! (constantly nil))
(defmethod persistence/persist! :test-persist [factory-id entity]
  (do-test-persist! factory-id entity))

(deftest test-override-persist
  (testing "not called by default"
    (with-redefs [do-test-persist! (fn [& args] (throw Exception))
                  do-persist! (fn [factory-id entity] entity)]
      (is (fab/create user))))
  (with-redefs [do-test-persist! (fn [factory-id entity]
                                   (is (= (:id user) factory-id)
                                       "overridden in build opts")
                                   entity)]
    (fab/create user {:persist-with :test-persist}))
  (with-redefs [do-test-persist! (fn [factory-id entity]
                                   (is (= (:id user) factory-id)
                                       "overridden in output opts")
                                   entity)]
    (fab/create user {} {:persist-with :test-persist})))
