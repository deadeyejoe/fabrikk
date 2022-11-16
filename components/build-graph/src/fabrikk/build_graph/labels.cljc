(ns fabrikk.build-graph.labels
  (:require [loom.attr :as attr]
            [loom.graph :as graph]
            [medley.core :as medley]
            [clojure.spec.alpha :as s]))

(s/def ::node any?)
(s/def ::attr-structure (s/map-of ::node (s/map-of ::node map?)))

(defn assoc-attr [attrs [src dest :as _edge] key value]
  (assoc-in attrs [src dest key] value))

(defn update-attr [attrs [src dest :as _edge] key f & args]
  (apply update-in attrs [src dest key] f args))

(defn assert-unique-labels-for-destinations [dest->attrs]
  (assert (->> (vals dest->attrs)
               (keep :labels)
               (mapcat vec)
               (apply distinct?))
          "Labels are not distinct"))

(defn assert-unique-labels
  ([attrs]
   (medley/map-vals assert-unique-labels-for-destinations attrs)
   attrs)
  ([attrs src]
   (let [dest->attrs (get attrs src)]
     (assert-unique-labels-for-destinations dest->attrs)
     attrs)))

(defn collect [attrs [src _dest :as edge] value]
  (-> (update-attr attrs edge :labels (fnil conj #{}) value)
      (assert-unique-labels src)))

(defn traverse-label
  "Returns the edge with source 'src' and the given label"
  [attrs src label]
  (when-let [found-dest (->> (get attrs src)
                             (some (fn [[dest attrs]]
                                     (when (-> (:labels attrs)
                                               (contains? label))
                                       dest))))]
    [src found-dest]))

(defn traverse-path
  "Traverses the given path of labels starting at 'src', and returns the edges 
   matching each one in turn. If any segment of the path doesn't exist, returns nil"
  [attrs src path]
  (when (seq path)
    (reduce (fn [edges label]
              (let [current-src (or (-> edges last last) src)
                    edge (traverse-label attrs current-src label)]
                (if edge
                  (conj edges edge)
                  (reduced nil))))
            []
            path)))

(defn merge-attrs [attrs other-attrs]
  ;; CURSED CODE
  (-> (merge-with (partial merge-with (partial merge-with into)) attrs other-attrs)
      (assert-unique-labels)))

(comment
  (-> {}
      (collect [1 2] :org)
      (collect [1 2] :org-email))
  (-> {}
      (collect [1 2] :org)
      (collect [1 3] :org))
  (let [attrs (-> {}
                  (collect [1 2] :org)
                  (collect [1 3] :user)
                  (collect [2 4] :saml)
                  (collect [3 2] :org))]
    [(traverse-label attrs 1 :org)
     (traverse-label attrs 1 :user)
     (traverse-label attrs 1 :blah)
     (traverse-label attrs 2 :saml)
     (traverse-path attrs 1 [:org :saml])
     (traverse-path attrs 1 [:user :org :saml])
     (traverse-path attrs 2 [:saml :method])
     (traverse-path attrs 2 [])
     (traverse-path attrs 2 nil)])
  (let [one (-> {}
                (collect [1 2] :org)
                (collect [1 3] :user))
        no-conflict (-> {}
                        (collect [1 2] :org-email)
                        (collect [2 4] :saml))]
    (merge-attrs one no-conflict))
  (let [one (-> {}
                (collect [1 2] :org)
                (collect [1 3] :user))
        conflict (-> {}
                     (collect [1 4] :org))]
    (merge-attrs one conflict)))

(comment
  "Examples of the loom functionality. It's a little limiting for our purposes"
  (-> (graph/digraph [1 2] [2 3] [1 4])
      (attr/add-attr 1 :label :one)
      (attr/add-attr [1 2] :label :foo)
      (attr/add-attr [2 3] :label :bar)
      (attr/add-attr [1 4] :label :baz))
  (-> (graph/digraph [1 2])
      (attr/add-attr 1 :label :one)
      (attr/add-attr [1 2] :label :foo)
      (attr/add-attr [1 2] :label :bar))
  (-> (graph/digraph [1 2])
      (attr/add-attr 1 :label 1)))
