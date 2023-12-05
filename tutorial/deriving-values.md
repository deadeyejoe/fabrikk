# Deriving Values

Some new requirements for app have come in:

* We want to anonymize admins even further. Their name should be of the form "Admin-X" where 'X' is the admin users id
* Each post should contain the name of its author

(both of these are bad ideas, but office politics forces you to accept them)

Function 'directives' (see [directives-101.md](directives-101.md "mention")) won't really help us here since they don't have access to the entity being built. We need a new directive, `derive`:

```clojure
(ns fab.tutorial
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
               :author-name (fab/derive [:author] :name}}))
```

`derive` allows us to, well, _derive_ values from either keys on the same entity or from any dependent entities created using `one`. It's first argument can be a key or a **path** - i.e. it identifies the source value to be derived from - the second is a function that's used to transform the source value into the derived value.

We'll go into more detail on paths later, for now it's enough to know that a path is a sequence of one or more keywords. Our path in the post factory is `[:author]` which can be translated to 'derive a value from the entity referenced through the author key', and we're using `:name` as the transform function to get the author's name.

Let's see this in action on the user entity first:

```clojure
(fab/build user {:traits [:admin]})
;; => {:id 1, :name "Admin-1", :email "admin-8575@example.com", :role "admin", :verified true}
(fab/build user {:with {:id 100}
                 :traits [:admin]})
;; => {:id 100, :name "Admin-100", :email "admin-8902@example.com", :role "admin", :verified true}
(fab/build user {:with {:name "Omega-Admin"}
                 :traits [:admin]})
;; => {:id 2, :name "Omega-Admin", :email "admin-7767@example.com", :role "admin", :verified true}
```

The derived value responds to changes in the value of the underlying key - if we specify the ID it gets reflected in the user's name. If we specify the name the derivation won't happen. Also, note that while in this case we're deriving a value in the admin trait from a key in the template, it's perfectly possible to derive a value in the template from a key in a trait.

Now for the post factory:

```clojure
(fab/build post)
;; => {:author 3,
;;     :id #uuid "233c7e97-f7cf-413b-89e3-69b782c9a4d8",
;;     :title "This one weird trick",
;;     :content "Some content goes here....",
;;     :author-name "John Smith"}

(let [jimmy (fab/build user {:with {:name "Jimmy Murphy"}})]
  (fab/build post {:with {:author jimmy}}))
;; => {:author 4,
;;     :id #uuid "564653ee-5e06-40bd-a5a0-8cf8b0032172",
;;     :title "This one weird trick",
;;     :content "Some content goes here....",
;;     :author-name "Jimmy Murphy"}
```

As before: if we change the name of the author user, we change the value of the author-name key. Now let's try:

```clojure
(fab/build post {:with {:author-name (fab/derive :author str)
                        :author-email (fab/derive [:author] :email)}})
;; => {:author 5,
;;     :id #uuid "781f0ca7-6399-46de-b056-3232cae71a98",
;;     :title "This one weird trick",
;;     :content "Some content goes here....",
;;     :author-name "5",
;;     :author-email "john@example.org"}
```

There are a few things to unpack here:

* The `derive` directive works happily in a `:with` option
* If we derive from the keyword `:author` instead of a path `[:author]`, we derive from the value of that key on the post entity we build i.e. the id of the author entity
* We can derive multiple values from the same dependent entity
