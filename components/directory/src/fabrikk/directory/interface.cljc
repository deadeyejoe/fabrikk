(ns fabrikk.directory.interface
  (:require [fabrikk.directory.core :as directory])
  (:refer-clojure :exclude [resolve]))

(defn register [category k v]
  (directory/register category k v))

(defn resolve [category k]
  (directory/resolve category k))

(defn register-factory [k v]
  (directory/register :factory k v))

(defn resolve-factory [k]
  (directory/resolve :factory k))
