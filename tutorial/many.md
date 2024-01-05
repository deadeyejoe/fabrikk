# Collections

So far we've been creating one entity at a time. What if we want to create multiple entities at once?

Let's recap our factories and persistence logic:

{% code lineNumbers="true" %}
```clojure
(ns fab.tutorial
  (:require [fabrikk.alpha.core :as fab]))

(defn admin-email []
  (str "admin-" (rand-int 10000) "@example.com"))

(def user
  (fab/->factory
   ::user
   {:primary-key :id
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
   ::post
   {:template {:id random-uuid
               :title "This one weird trick"
               :content "Some content goes here...."
               :author (fab/one ::user)
               :author-name (fab/derive [:author] :name)}}))

(defonce my-store (atom {}))

(def collect (fnil conj []))

(defmethod fab/persist! :my-store [factory-id entity]
  (let [persisted (assoc entity :id (case factory-id
                                      ::user (rand-int 1000000)
                                      ::post (random-uuid)))]
    (swap! my-store update factory-id collect persisted)
    persisted))
(fab/set-default-persistence :my-store)
```
{% endcode %}

## Building Collections

We've already met the `build-list` function in [referencing-entities.md](referencing-entities.md "mention"), but let's recap it here. It's a function that accepts a factory, a quantity, and the same build options as `build` and (as you might expect), builds that quantity of the entity. It's important to note that by default a new user is built for each post, which might not be what you want. Let's create a list of posts with the same user:

```clojure
(let [author (fab/build user {:with {:name "Jimmy Smith"}})]
  (fab/build-list post 5 {:with {:author author
                                 :title "You won't believe what happens next!"}}))
;; => [{:author 6,
;;      :id #uuid "bcefaa04-1de1-4a66-b43b-e3dcc8c70037",
;;      :title "You won't believe what happens next!",
;;      :content "Some content goes here....",
;;      :author-name "Jimmy Smith"}
;;     {:author 6,
;;      :id #uuid "c334c0ca-6458-4af9-9c2f-463f625f1789",
;;      :title "You won't believe what happens next!",
;;      :content "Some content goes here....",
;;      :author-name "Jimmy Smith"}
;;     {:author 6,
;;      :id #uuid "b89be1b3-0b55-45ed-89cb-dfe3818d514c",
;;      :title "You won't believe what happens next!",
;;      :content "Some content goes here....",
;;      :author-name "Jimmy Smith"} ... (and 2 more) ...
```

Here we can see that the map passed to the `with` build option are applied to each post, and they all have the same author and title.

## Creating Collections

Now let's look at persisting multiple posts using `create-list`:

```clojure
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
;;      :author-name "User-6912"} ... (and 2 more) ...
```

Again, by default fabrikk will create a separate user for each post. And we can create posts for one user with:

```clojure
(let [author (fab/build user)]
  (->> (fab/create-list post 5 {:with {:author author}})
       (map :author)))
;; => (974 974 974 974 974)
```

## Mixing building and creation

The headline here is:

{% hint style="info" %}
<mark style="color:red;background-color:red;">BE VERY CAUTIOUS WHEN MIXING build AND create</mark>

(hopefully the ugliness of this warning will make it stick out more in your mind)
{% endhint %}

The previous code block mixed `build` with `create`, let's look at why this might be a bit of a footgun:

```clojure
(let [author (fab/build user)]
  [(->> (fab/create-list post 5 {:with {:author author}})
        (map :author))
   (->> (fab/create-list post 5 {:with {:author author}})
        (map :author))])
;; => [(2897 2897 2897 2897 2897) (453 453 453 453 453)]
```

Here we've built a user, and then used it in two calls to `create-list`. The first call will create a user and assign it as the author for each post, but when it comes to the second call, fabrikk has no way to link it to the previously created user. This means each list of posts has a different author. This might be what you want in some situations but might blow your foot off in others.

If you use the same built entity multiple times in one create call:

```clojure
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
```

fabrikk is clever enough to create one user and point both fields to it.

Finally, when fabrikk creates an entity it marks it as created (in the metadata), and will not create it again if it's passed to a different create call.

```clojure
(let [author (fab/create user)]
  [author
   (->> (fab/create-list post 5 {:with {:author author
                                        :editor author}})
        (map :author))])
;; => [{:id 401125, :name "John Smith", :email "john@example.org", :role "user", :verified true}
;;     (401125 401125 401125 401125 401125)]
```

## The 'many' Directive

In some situations, you may want an entity to reference a list of entities. We can do this using the list version of the `one` directive: `many`. Let's introduce a new factory that uses it:

```clojure
(def group
  (fab/->factory
   ::group
   {:primary-key :id
    :template {:id random-uuid
               :name "Group"
               :members (fab/many ::user 3)}}))

(fab/build group)
;; => {:id #uuid "1bfaa980-6f0d-43c7-b485-dfa5d0e3c23a", 
;;     :name "Group",
;;     :members [12 13 14]}
```

A cool thing about `many` is that the `derive` directive knows how to handle it, allowing you to access the entities in the list via their index:

```clojure
(fab/build group {:with {:leader (fab/derive [:members 0] :name)}})
;; => {:members [15 16 17], 
;;     :id #uuid "0b409fd4-19b3-4119-8701-b20529837c74", 
;;     :name "Group", 
;;     :leader "John Smith"}
```

Also, remember that we haven't created a concrete association between groups and users, we've just specified that the default group representation requires three users. We're free to override this:

```clojure
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
```

Here we've built a group of posts instead of users. Since the post factory doesn't specify a `primary-key` the full post entity is used instead.
