---
description: An entity generation library for Clojure
---

# Fabrikk

## Introduction

Fabrikk is a pure clojure library for generating realistic data for your tests or development workflow. It lets you quickly build entities from your application domain by defining a factory for each one, provides a high degree of control over how they're constructed, and models associations between them.

Fabrikk aims to let you specify only the data that's important to your current use case, and to handle all the rest of the details, including building associated entities. As an example: if your post requires an author user, Fabrikk can build one automatically when you build a post. It can output these hierarchies of entities as pure data, or it can be integrated with your persistence layer to build each entity in the correct order.

## Usage

Include it in your `deps.edn` (see clojars for other dependency management mechanisms)

```clojure
org.clojars.joe-douglas/fabrikk {:mvn/version "0.0.53"}
```

Require it in your namespace:

```clojure
(ns demo
  (:require [fabrikk.alpha.core :as fab]))
```

Then write some factories:

```clojure
(def user
  (fab/->factory
    {:id ::user
     :template {:name "John Smith"
                :email "john@example.org"
                :role "admin"}}) 
```

And start building:

```clojure
(fab/build user {:with {:name "John Murphy"}})
;; => {:name "John Murphy", :email "john@example.org", :role "admin"}
```

Sound good? Then jump straight into the [tutorial](broken-reference)!
