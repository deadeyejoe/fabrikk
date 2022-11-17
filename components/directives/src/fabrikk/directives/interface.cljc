(ns fabrikk.directives.interface
  (:require [fabrikk.directives.core :as core])
  (:refer-clojure :exclude [sequence]))

(defn constant [x]
  (core/constant x))

(defn sequence [f identifier]
  (core/sequence f identifier))

(defn build [factory opts]
  (core/build factory opts))

(defn build-list [factory n opts]
  (core/build-list factory n opts))

(defn derive [key-or-path f]
  (core/derive key-or-path f))
