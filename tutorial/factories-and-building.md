# Factories & Building

First we'll learn how to create factories and how to use them to build entities. Let's say our application has users that we want to create for use in our tests, and each user has:

* a name
* an email
* a role - one of 'admin' or 'user'

We might write our factory definition as:

```clojure
(ns fab.tutorial
  (:require [fabrikk.core.alpha :as fab]))

(def user
  (fab/->factory
    {:template {:name "John Smith"
                :email "john@example.org"
                :role "admin"}}) 
```

A factory definition is a map with at least a `template` key:

* The id uniquely identifies the factory, it's best to use a namespaced keyword for this. This doesn't do much for us yet but will be handy later
* The template details the keys that our entity will have, and the values of those keys

We'll explore more options in the course of this tutorial, and you can take a look at [#factory-definition](../reference/api.md#factory-definition "mention") to see a comprehensive list.

Now that we have a  factory we can get building:

```clojure
(fab/build user) 
;; => {:name "John Smith", :email "john@example.org", :role "admin"}
(fab/build user) 
;; => {:name "John Smith", :email "john@example.org", :role "admin"}
```

But wait, this generates the same user every time! We can pass _build options_ to `build` to vary the output, using the `with` option:

```clojure
;; we use 'with' to change the name:
(fab/build user {:with {:name "John Murphy"}})
;; => {:name "John Murphy", :email "john@example.org", :role "admin"}

;; or add an arbitrary key to the entity:
(fab/build user {:with {:favourite-food "chips"}})
;; => {:name "John Smith", :email "john@example.org", :role "admin", 
;;     :favourite-food "chips"}
```
