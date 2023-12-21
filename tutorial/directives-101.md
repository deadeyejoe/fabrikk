# Directives 101

We've now done some thinking and want to give our users an id instead of identifying them by email address. We're also a bit bored of seeing the same admin email address again and again.

Let's update our factory again:

{% code lineNumbers="true" %}
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
    :traits {:admin {:email admin-email
                     :role "admin"}
             :unverified {:verified false}}}))
```
{% endcode %}

Now things are getting more interesting! We've introduced our first _directives -_ special instructions to fabrikk.&#x20;

The `sequence` directive provides an increasing series of integers. Perfect for giving each user a unique id:

{% code overflow="wrap" %}
```clojure
(fab/build user)
;; => {:id 1, :name "John Smith", :email "john@example.org", :role "user", :verified true}

(fab/build user)
;; => {:id 2, :name "John Smith", :email "john@example.org", :role "user", :verified true}
```
{% endcode %}

I said directive**s** plural above, so where's the other one? Well, technically, functions are directives too! When a template value is a zero argument function, fabrikk will invoke that function at build time and set the key to the return value.&#x20;

```clojure
(fab/build-list user 2 {:traits [:admin]})
;; => [{:id 3, :name "John Smith", :email "admin-9325@example.com", :role "admin", :verified true}
;;     {:id 4, :name "John Smith", :email "admin-1038@example.com", :role "admin", :verified true}]

```

Note that the admin-email function was specified in a trait,  the following is true in general:

{% hint style="info" %}
Anything that works for a template will work when specified in a trait.
{% endhint %}

Note that functions don't allow you to access the entity you're building, in the next section we'll cover another directive that lets you do that.
