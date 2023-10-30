# Referencing Entities

It's unlikely that we're building an application that deals solely with users, let's be super imaginative and say we're building a.... revolutionary blogging platform! We'll add a `post` factory:

{% code lineNumbers="true" %}
```clojure
(ns reference-demo
  (:require [fabrikk.alpha.core :as fab]))

(defn admin-email []
  (str "admin-" (rand-int 10000) "@example.com"))

(def user
  (fab/->factory
   {:id ::user
    :template {:id (fab/sequence)
               :name "John Smith"
               :email "john@example.org"
               :role "user"
               :verified true}
    :traits {:admin {:email admin-email
                     :role "admin"}
             :unverified {:verified false}}}))

(def post
  (fab/->factory
   {:id ::post
    :template {:id random-uuid
               :title "This one weird trick"
               :content "Some content goes here...."
               :author (fab/one ::user)}}))
```
{% endcode %}

To switch things up a little, posts are identified by a uuid, so we can use clojure core's `random-uuid` function to generate one. Posts also have a `title` , some `content` , and an `author`. It's here we meet our next directive: `one`.

The `one` directive tells fabrikk that the current entity depends on an entity built by another factory. In this case we've given it the id of our user factory (which we mentioned in [factories-and-building.md](factories-and-building.md "mention")), so when we build a post, it'll automatically build a user:

```clojure
(fab/build post)
;; => {:author {:id 2, :name "John Smith", :email "john@example.org", :role "user", :verified true},
;;     :id #uuid "afe9eb8d-4722-4718-ab42-5fc50d766473",
;;     :title "This one weird trick",
;;     :content "Some content goes here...."}
```

... and nest the full user entity under the author key. There are some cases where we'd want to do this - the entity we're creating might have some complex sub structures - but in general this isn't what we want. Our users and our posts will probably be stored as two separate entities in our database layer, and we'll reference them by id. A small tweak to the user factory solves this:

```clojure
(def user
  (fab/->factory
   {:id ::user
    :primary-id :id
    :template {:id (fab/sequence)
               :name "John Smith"
               :email "john@example.org"
               :role "user"
               :verified true}
    :traits {:admin {:email admin-email
                     :role "admin"}
             :unverified {:verified false}}}))
```

Fabrikk doesn't make any assumptions about what key identifies a user, so you can tell it to identify them by their `id` by specifying a `primary-id`. Now let's try bulding another post:

```clojure
(fab/build post)
;; => {:author 3,
;;     :id #uuid "67a43dea-f992-47f7-a78a-761a29de9f1c",
;;     :title "This one weird trick",
;;     :content "Some content goes here...."}
```

So now we're referencing something that looks like a user id, but where's the user? Let's introduce our first output option:

```clojure
(fab/build post {} {:output-as :build-order})
;; => ({:id 4, :name "John Smith", :email "john@example.org", :role "user", :verified true}
;;     {:author 4,
;;      :id #uuid "83ee842e-ee37-4dd1-b385-6aba8f7a6c0f",
;;      :title "This one weird trick",
;;      :content "Some content goes here...."})
```

Output options are a third optional argument to `build` (and related functions), in this case we're not supplying any build options so our second argument is an empty hash.

There's our missing user! It's here that I reveal that the output of `(fab/build post)` gives you an entity map, but there's a _**build graph**_ hiding in the [metadata](https://clojure.org/reference/metadata) of that map. The build graph keeps track of the entities that the output depends on, and the `build-order` option outputs a reverse [topological sort](https://en.wikipedia.org/wiki/Topological\_sorting) of that graph.

We can take a look at the build graph by getting the metadata of a post:

```clojure
(metadata (fab/build post))
;; => {:node-id->value
;;     {#uuid "36b2f444-d8e1-422e-b21a-c01dc1c4af81"
;;      {:uuid #uuid "36b2f444-d8e1-422e-b21a-c01dc1c4af81",
;;      ............. and a ton more stuff
```

Yikes! There's a lot going on there, but you shouldn't need to worry about these details. This might not seem very useful yet, but it will be soon when we talk about using fabrikk to persist the entities it builds.

What happens when we want a list of posts?

```clojure
(fab/build-list post 2)
;; => [{:author 5,
;;      :id #uuid "8f3b46d2-2efd-4689-82c2-23f62db30886",
;;      :title "This one weird trick",
;;      :content "Some content goes here...."}
;;     {:author 6,
;;      :id #uuid "36c0a332-b878-4527-a9df-3e6460c455bf",
;;      :title "This one weird trick",
;;      :content "Some content goes here...."}]
```

We can see here that fabrikk has created a new user for each post. If you want the same user for each post you need to be explicit about it:

```clojure
(let [author (fab/build user)]
  (fab/build-list post 2
                  {:with {:author author}}
                  {:output-as :build-order}))
;; => ({:id 7, :name "John Smith", :email "john@example.org", :role "user", :verified true}
;;     {:author 7,
;;      :id #uuid "593186f2-254d-4a4a-acde-f8a2595e00bc",
;;      :title "This one weird trick",
;;      :content "Some content goes here...."}
;;     {:author 7,
;;      :id #uuid "fb0d88e9-fd82-4730-adf3-ddb5b532396f",
;;      :title "This one weird trick",
;;      :content "Some content goes here...."})
```

Using the build-order output again, we can see that both posts will share the same user.&#x20;

A final note for this section: even though we've used the `one` directive in the post factory, the relationship we're describing isn't between the factories. All we're saying is that the default representation of a post requires that a user be created. We're free to override that if we wish:

```clojure
(fab/build post {:with {:author "John"}} {:output-as :build-order})
;; => ({:author "John",
;;      :id #uuid "33cdad95-5235-4a5a-a555-64a30f7ee659",
;;      :title "This one weird trick",
;;      :content "Some content goes here...."})
```

Or add additional dependent entities at will:

```clojure
(fab/build post {:with {:editor (fab/one ::user)}})
;; => {:author 8,
;;     :editor 9,
;;     :id #uuid "f99f03e6-270c-4aa1-924a-08606857fe0e",
;;     :title "This one weird trick",
;;     :content "Some content goes here...."}
```
