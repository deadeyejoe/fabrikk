(ns joe.tutorial.alpha.collections
  (:require [fabrikk.alpha.core :as fab]))

(defn admin-email []
  (str "admin-" (rand-int 10000) "@example.com"))

(def user
  (fab/->factory
   {:id ::user
    :primary-id :id
    :template {:id (fab/sequence)
               :name "John Smith"
               :email "john@example.org"
               :role "user"
               :verified true}
    :traits {:admin {:name (fab/derive :id (partial str "Admin-"))
                     :email admin-email
                     :role "admin"}
             :unverified {:verified false}}}))

(def post
  (fab/->factory
   {:id ::post
    :template {:id random-uuid
               :title "This one weird trick"
               :content "Some content goes here...."
               :author (fab/one ::user)
               :author-name (fab/derive [:author] :name)}}))

(defonce my-store (atom {}))
(reset! my-store {})
(def collect (fnil conj []))

(defmethod fab/persist! :my-store [factory-id entity]
  (let [persisted (assoc entity :id (case factory-id
                                      ::user (rand-int 1000000)
                                      ::post (random-uuid)))]
    (swap! my-store update factory-id collect persisted)
    persisted))
(fab/set-default-persistence :my-store)

(let [author (fab/build user {:with {:name "Jimmy Smith"}})]
  (fab/build-list post 5 {:with {:author author
                                 :title "You won't believe what happens next!"}}))
;; => [{:author 1,
;;      :id #uuid "bcefaa04-1de1-4a66-b43b-e3dcc8c70037",
;;      :title "You won't believe what happens next!",
;;      :content "Some content goes here....",
;;      :author-name "Jimmy Smith"}
;;     {:author 1,
;;      :id #uuid "c334c0ca-6458-4af9-9c2f-463f625f1789",
;;      :title "You won't believe what happens next!",
;;      :content "Some content goes here....",
;;      :author-name "Jimmy Smith"}
;;     {:author 1,
;;      :id #uuid "b89be1b3-0b55-45ed-89cb-dfe3818d514c",
;;      :title "You won't believe what happens next!",
;;      :content "Some content goes here....",
;;      :author-name "Jimmy Smith"}
;;     {:author 1,
;;      :id #uuid "8e72fe0f-30e1-46be-b1e4-fa5f05b6b2c4",
;;      :title "You won't believe what happens next!",
;;      :content "Some content goes here....",
;;      :author-name "Jimmy Smith"}
;;     {:author 1,
;;      :id #uuid "2bc6c5bb-7a24-45f0-bed2-632c862f4288",
;;      :title "You won't believe what happens next!",
;;      :content "Some content goes here....",
;;      :author-name "Jimmy Smith"}]

(fab/create-list post 5)
;; => [{:author 5707,
;;      :id #uuid "1cca3017-51ac-4fa2-b16c-4179b37f80c7",
;;      :title "This one weird trick",
;;      :content "Some content goes here....",
;;      :author-name "User-7025"}
;;     {:author 3302,
;;      :id #uuid "7dc42c7c-033a-45f3-b3ec-f6a0d91bc1bd",
;;      :title "This one weird trick",
;;      :content "Some content goes here....",
;;      :author-name "User-734"}
;;     {:author 89,
;;      :id #uuid "41d57abd-ed4a-419a-8a65-ee63406b5d62",
;;      :title "This one weird trick",
;;      :content "Some content goes here....",
;;      :author-name "User-6912"}
;;     {:author 5798,
;;      :id #uuid "694fbcb0-fcd6-4bb7-accc-f7a943d37292",
;;      :title "This one weird trick",
;;      :content "Some content goes here....",
;;      :author-name "User-9509"}
;;     {:author 2312,
;;      :id #uuid "a172477b-0cc5-40ae-a57f-4317f603f28b",
;;      :title "This one weird trick",
;;      :content "Some content goes here....",
;;      :author-name "User-3509"}]

(let [author (fab/build user)]
  (->> (fab/create-list post 5 {:with {:author author}})
       (map :author)))
;; => (974 974 974 974 974)


(let [author (fab/build user)]
  [(->> (fab/create-list post 5 {:with {:author author}})
        (map :author))
   (->> (fab/create-list post 5 {:with {:author author}})
        (map :author))])
;; => [(2897 2897 2897 2897 2897) (453 453 453 453 453)]

(let [author (fab/create user)]
  [(->> (fab/create-list post 5 {:with {:author author}})
        (map :author))
   (->> (fab/create-list post 5 {:with {:author author}})
        (map :author))])
;; => [(7163 7163 7163 7163 7163) (7163 7163 7163 7163 7163)]

(let [author (fab/build user)]
  [author
   (fab/create post {:with {:author author
                            :editor author}})])
;; => [{:id 10, :name "John Smith", :email "john@example.org", :role "user", :verified true}
;;     {:author 323043,
;;      :id #uuid "d52d7393-47cd-4aa1-9e3e-5e436e9a3bf8",
;;      :title "This one weird trick",
;;      :content "Some content goes here....",
;;      :editor 323043,
;;      :author-name "John Smith"}]

(let [author (fab/create user)]
  [author
   (->> (fab/create-list post 5 {:with {:author author
                                        :editor author}})
        (map :author))])
;; => [{:id 401125, :name "John Smith", :email "john@example.org", :role "user", :verified true}
;;     (401125 401125 401125 401125 401125)]

(def group
  (fab/->factory
   {:id ::group
    :primary-id :id
    :template {:id random-uuid
               :name "Group"
               :members (fab/many ::user 3)}}))

(fab/build group)
;; => {:id #uuid "1bfaa980-6f0d-43c7-b485-dfa5d0e3c23a", 
;;     :name "Group",
;;     :members [12 13 14]}

(fab/build group {:with {:leader (fab/derive [:members 0] :name)}})
;; => {:members [51 52 53], 
;;     :id #uuid "0b409fd4-19b3-4119-8701-b20529837c74", 
;;     :name "Group", 
;;     :leader "John Smith"}

(fab/build group {:with {:members (fab/many ::post 2)}})
;; => {:members
;;     [{:author 22,
;;       :id #uuid "98c5dbd6-fb04-43f6-a917-82e6d1652622",
;;       :title "This one weird trick",
;;       :content "Some content goes here....",
;;       :author-name "John Smith"}
;;      {:author 23,
;;       :id #uuid "97d6d428-2773-486b-8cbb-a3beca00aa3d",
;;       :title "This one weird trick",
;;       :content "Some content goes here....",
;;       :author-name "John Smith"}],
;;     :id #uuid "01c41dde-46f4-4a3f-82f2-4fcde2934e4c",
;;     :name "Group"}
