# Traits

Let's flesh out our user model a little:&#x20;

* We've implemented email verification so users have a `verified` flag indicating whether their email has been verified&#x20;
* After some of our admins were harassed we've decided to give their accounts anonymized email addresses
* We've also noticed that we're writing tests for users more frequently, so we want to change the default role in the factory to "user"

Now our email key is linked to the role of the user, so we might want to control these two together. We can use `traits` to do this. Traits are like little snippets of a template we can control with a single keyword. Let's update our factory definition a little:

{% code lineNumbers="true" %}
```clojure
(ns trait-demo
 (:require [fabrikk.alpha.core :as fab]))
 
(def user
  (fab/->factory
   {:id ::user
    :template {:name "John Smith"
               :email "john@example.org"
               :role "user"
               :verified true}
    :traits {:admin {:email "admin-0001@example.org"
                     :role "admin"}
             :unverified {:verified false}}}))
```
{% endcode %}

We've added a `traits` key to the factory definition, and added two traits `admin` and `unverified`. Now we can use the traits as shorthand when we build by specifying them in the build options:

{% code overflow="wrap" %}
```
(fab/build user)
;; => {:name "John Smith", :email "john@example.org", :role "user", :verified true}

(fab/build user {:traits [:admin]})
;; => {:name "John Smith", :email "admin-0001@example.org", :role "admin", :verified true}
```
{% endcode %}

Traits can be composed:

{% code overflow="wrap" %}
```
(fab/build user {:traits [:admin :unverified]})
;; => {:name "John Smith", :email "admin-0001@example.org", :role "admin", :verified false}
```
{% endcode %}
