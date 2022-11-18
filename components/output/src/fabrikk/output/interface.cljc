(ns fabrikk.output.interface
  (:require [fabrikk.output.core :as output]))

(def build output/build)

(defn as-meta []
  (output/as-meta))

(defn as-value
  ([] (output/as-value nil))
  ([transform]
   (output/as-value transform)))

(defn as-context []
  (output/as-context))

(defn as-tuple []
  (output/as-tuple))

(defn as-collection []
  (output/as-collection))
