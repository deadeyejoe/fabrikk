(ns fabrikk.directory.core
  (:refer-clojure :exclude [resolve]))

(defonce store (atom {}))

(comment
  @store)

(defn resolve [category k]
  (get-in @store [category k]))

(defn register [category k v]
  (swap! store assoc-in [category k] v))
