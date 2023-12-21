(ns fabrikk.build-context.interface-test
  (:require [clojure.test :as test :refer :all]
            [fabrikk.build-context.interface :as build-context]
            [fabrikk.entity.interface :as entity]
            [loom.graph :as graph]
            [loom.alg :as graph-alg]
            [loom.io :as lio]
            [fabrikk.build-context.interface :as context]))

(def post-factory {:id ::post})
(def post {:title "How to test fabrikk"})

(def user-factory {:id ::user
                   :primary-key :id})
(def joe {:id 1
          :name "Joe"})
(def john {:id 2
           :name "John"})
(def jane {:id 2
           :name "Jane"})

(defn entity-with-value
  ([factory value] (entity-with-value factory {} value))
  ([factory build-opts value]
   (-> (entity/create! factory build-opts)
       (entity/update-value (constantly value)))))

(deftest test-associate-entity
  (let [post-entity (entity-with-value post-factory post)
        post-context (build-context/init post-entity)]
    (testing "when associated entity has no build-options"
      (let [user-entity (entity-with-value user-factory joe)
            associated (build-context/associate-entity post-context :author user-entity)]
        (is (entity/id-match? post-entity (build-context/primary associated))
            "Preserves primary")
        (is (= (:id joe) (-> associated (build-context/primary) (entity/value) :author))
            "Updates primary value")
        (is (= user-entity (build-context/id->entity associated (entity/id user-entity)))
            "Contains associated entity")))
    (testing "when associated entity has build-options"
      (let [user-entity (entity-with-value user-factory {:associate-as :name} joe)
            associated (build-context/associate-entity post-context :author user-entity)]
        (is (= (:name joe) (-> associated (build-context/primary) (entity/value) :author))
            "Updates primary with correct value")))
    (testing "when context contains other entities"
      (let [moderator-entity (entity-with-value user-factory john)
            author-entity (entity-with-value user-factory joe)
            associated (-> post-context
                           (build-context/associate-entity :moderator moderator-entity)
                           (build-context/associate-entity :author author-entity))]
        (is (= (:id joe) (-> associated (build-context/primary) (entity/value) :author))
            "Author matches")
        (is (= (:id john) (-> associated (build-context/primary) (entity/value) :moderator))
            "Moderator matches")
        (is (= 2 (-> associated graph/edges count))
            "Creates links")))
    (testing "when creating multiple links to the same entity"
      (let [user-entity (entity-with-value user-factory joe)
            associated (-> post-context
                           (build-context/associate-entity :author user-entity)
                           (build-context/associate-entity :author-name (entity/override-association user-entity :name)))
            {:keys [author author-name]} (-> associated (build-context/primary) (entity/value))]
        (is (= (:id joe) author) "Author matches")
        (is (= (:name joe) author-name) "Author name matches")
        (is (= 1 (-> associated graph/edges count)) "Creates one edge per entity pair")))
    (testing "when specifying a source entity"
      (let [author-entity (entity-with-value user-factory joe)
            editor-entity (entity-with-value user-factory john)
            moderator-entity (entity-with-value user-factory jane)
            post-context (build-context/associate-entity post-context :author author-entity)]
        (testing "that exists in the graph"
          (let [associated (build-context/associate-entity post-context author-entity :editor editor-entity)]
            (is (= (:id john) (-> associated
                                  (build-context/id->entity (entity/id author-entity))
                                  (entity/value)
                                  :editor)))))
        (testing "that does not exist in the graph"
          (is (thrown? AssertionError
                       (build-context/associate-entity post-context moderator-entity :editor editor-entity))))))))

(deftest test-associate-context
  (let [initial-author-entity (entity-with-value user-factory joe)
        editor-entity (entity-with-value user-factory john)
        author-context (-> (build-context/init initial-author-entity)
                           (build-context/associate-entity :editor editor-entity))
        author-entity (build-context/primary author-context)

        moderator-entity (entity-with-value user-factory jane)
        post-context (-> (build-context/init (entity-with-value post-factory post))
                         (build-context/associate-entity :moderator moderator-entity))
        post-entity (build-context/primary post-context)

        associated (build-context/associate-context post-context :author author-context)]
    (is (entity/id-match? post-entity (build-context/primary associated))
        "Preserves primary")
    (is (= (:id joe) (-> associated (build-context/primary) (entity/value) :author))
        "Updates primary value")
    (testing "Merges other entities as-is"
      (is (= author-entity (build-context/id->entity associated (entity/id author-entity))))
      (is (= editor-entity (build-context/id->entity associated (entity/id editor-entity))))
      (is (= moderator-entity (build-context/id->entity associated (entity/id moderator-entity)))))))

(deftest test-propagate
  ;; TODO: fix
  (let [editor-entity (entity-with-value user-factory john)
        author-entity (entity-with-value user-factory joe)
        post-context (-> (build-context/init (entity-with-value post-factory post))
                         (build-context/associate-entity :author author-entity)
                         (build-context/associate-entity author-entity :editor editor-entity)
                         (build-context/associate-entity author-entity
                                                         :editor-name
                                                         (entity/override-association editor-entity :name)))
        updated-editor (-> editor-entity
                           (entity/override-association :name)
                           (entity/update-value assoc :name "jim"))
        propagated (build-context/propagate post-context updated-editor)]
    (is (= "jim" (-> (build-context/id->entity propagated (entity/id author-entity)) (entity/value) :editor-name))
        "Updates changed derived values")
    (is (= 2 (-> (build-context/id->entity propagated (entity/id author-entity)) (entity/value) :editor))
        "Non derived values are unaffected")
    (is (= (build-context/primary post-context) (build-context/primary propagated))
        "Only performs a single step")))

(deftest test-traverse-path
  (let [author-entity (entity-with-value user-factory joe)
        editor-entity (entity-with-value user-factory john)
        list-entity (entity/create-list!)
        [m1 m2 m3] (map #(entity-with-value user-factory {:id %}) (range 1 4))
        context (-> (build-context/init (entity-with-value post-factory post))
                    (build-context/associate-entity :author author-entity)
                    (build-context/associate-entity :editor editor-entity)
                    (build-context/associate-entity author-entity :editor editor-entity)
                    (build-context/associate-entity :moderators list-entity)
                    (build-context/associate-entity list-entity 0 m1)
                    (build-context/associate-entity list-entity 1 m2)
                    (build-context/associate-entity list-entity 2 m3)
                    (build-context/associate-entity m3 :editor editor-entity))]
    (is (entity/id-match? (build-context/primary context)
                          (build-context/traverse-path context nil)) "Nil path gives primary")
    (is (entity/id-match? (build-context/primary context)
                          (build-context/traverse-path context [])) "Empty path gives primary")
    (is (entity/id-match? author-entity
                          (build-context/traverse-path context [:author])))
    (is (entity/id-match? list-entity
                          (build-context/traverse-path context [:moderators])))
    (is (entity/id-match? m1
                          (build-context/traverse-path context [:moderators 0])))
    (testing "Multiple paths supported")
    (is (entity/id-match? editor-entity
                          (build-context/traverse-path context [:editor])))
    (is (entity/id-match? editor-entity (build-context/traverse-path context [:author :editor])))
    (is (entity/id-match? editor-entity (build-context/traverse-path context [:moderators 2 :editor])))))

(deftest path-to-test
  (let [author-entity (entity-with-value user-factory joe)
        editor-entity (entity-with-value user-factory john)
        list-entity (entity/create-list!)
        [m1 m2 m3] (map #(entity-with-value user-factory {:id %}) (range 1 4))
        context (-> (build-context/init (entity-with-value post-factory post))
                    (build-context/associate-entity :author author-entity)
                    (build-context/associate-entity :name-of-author (entity/override-association author-entity :name))
                    (build-context/associate-entity :editor editor-entity)
                    (build-context/associate-entity author-entity :editor editor-entity)
                    (build-context/associate-entity :moderators list-entity)
                    (build-context/associate-entity list-entity 0 m1)
                    (build-context/associate-entity list-entity 1 m2)
                    (build-context/associate-entity list-entity 2 m3)
                    (build-context/associate-entity m3 :editor editor-entity))]
    (is (= [[:editor]] (build-context/path-to context editor-entity)) "Finds the shortest path")
    (is (= [[:author :name-of-author]] (build-context/path-to context author-entity)) "Returns multi-labels")
    (is (= [[:moderators] [2]] (build-context/path-to context m3)) "Returns multiple segments")))
