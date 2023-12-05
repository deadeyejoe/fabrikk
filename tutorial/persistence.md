# Persistence

So far everything we've done has been geared towards building our entities as pure data, but there's a good chance you'll want to write tests that involve your persistence layer. Wouldn't be nice to be able to use fabrikk to persist the entities it creates? That's what we're going to cover in this section of the tutorial.

Let's recap the current state of our factories:

```clojure
(ns fab.tutorial
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
```

To support persistence we need to write some code that connects fabrikk to our persistence code. No changes to our factories or the persistence code should be required. This might be surprising: our factories build entities with ids, and references entities using these ids, but frequently our persistence layer is in control of setting these values, so won't this cause problems? No. Let's find out why.

Since this is a tutorial we'll use a simple atom to persist our entities, the persistence code looks like this:

{% code overflow="wrap" %}
```clojure
(defonce my-store (atom {})) 
;; our simulated persistence layer

(def collect (fnil conj [])) 
;; a little trick so we don't have to specify the entity collections we'll create in advance

(defmethod fab/persist! :my-store [factory-id entity]
  (let [persisted (assoc entity :id (case factory-id
                                      ::user (rand-int 1000000)
                                      ::post (random-uuid)))]
    (swap! my-store update factory-id collect persisted)
    persisted))
;; the business end

(fab/set-default-persistence :my-store)
;; use our custom persistence method by default
```
{% endcode %}

Fabrikk exposes a `persist!` multimethod so you can connect it to your persistence layer. It is expected to take 2 arguments - the ID of the factory and the entity we're creating - and return the persisted entity. To persist an entity we simply collect it in a vector using the factory id as a key. To simulate not being in control of our ids, we assign them randomly.

Now let's see `create` in action:

```clojure
(fab/create post)

@my-store
;; => {:user
;;     [{:id 846761,
;;       :name "John Smith",
;;       :email "john@example.org",
;;       :role "user",
;;       :verified true}],
;;     :post
;;     [{:author 846761,
;;       :id #uuid "e9382f32-32b4-47fb-8c6c-089ca0a6a025",
;;       :title "This one weird trick",
;;       :content "Some content goes here....",
;;       :author-name "John Smith"}]}
```

`create` takes all the same arguments as build, persists the entities (in build order), and returns the persisted entity (or entities depending on what options you pass to create, it supports the same `output-as` option as `build`).

Note that the post is referencing the author via its randomly assigned id. The build graph we mentioned earlier in [referencing-entities.md](referencing-entities.md "mention") allows us to create entities in the correct order i.e. user->post, and allows us to _**propagate**_ the change to the user our persistence layer makes to the post.

This propagation also applies to all values calculated using fabrikk's `derive` directive. Let's tweak our persistence function so the user name is also out of our control:

```clojure
(defmethod fab/persist! :my-store [factory-id entity]
  (let [persisted (case factory-id
                    ::user (assoc entity
                                  :id (rand-int 10000)
                                  :name (str "User-" (rand-int 10000)))
                    ::post (assoc entity :id (random-uuid)))]
    (swap! my-store update factory-id collect persisted)
    persisted))

(fab/create post)
;; => {:author 4416,
;;     :id #uuid "92fd0318-19e7-4e4a-bee9-d555c99da9e3",
;;     :title "This one weird trick",
;;     :content "Some content goes here....",
;;     :author-name "User-5570"}
```

The user name is based on a different randomly chosen integer from the user id, and this randomly chosen name is also propagated to the post before it's persisted.

In our simple example so far we only have 2 types of entities in 2 'layers' to worry about, but since fabrikk is working with a graph under the hood, we can support arbitrarily complex hierarchies.
