(ns fabrikk.build-graph.link-graph
  (:refer-clojure :exclude [merge]))

(def associate-index 0)
(def label-index 1)

(defn link [link-graph source [label associate-as :as _link] target]
  (assoc-in link-graph [source label] [associate-as target]))

(defn conflict? [link-graph source [label _associate-as :as _link] target]
  (= (get-in link-graph [source label label-index]) target))

(def no-conflict? (complement conflict?))

(defn link-no-conflict! [link-graph source link* target]
  (assert (no-conflict? link-graph source link* target))
  (link link-graph source link* target))

(defn merge [graph other-graph]
  (merge-with clojure.core/merge graph other-graph))

(defn inward-links [link-graph node]
  (mapcat (fn [[source link->dest]]
            (keep (fn [[label [associate-as dest]]]
                    (when (= node dest)
                      [source [label associate-as] dest]))
                  link->dest))
          link-graph))

(comment
  (link {} 1 [:org :id] 2)
  (link {} 1 [:org :b] 1)
  (link-no-conflict! {1 {:org [:a 2]}} 1 [:org :b] 2)
  (->> (inward-links (merge (-> {}
                                (link 1 [:user (fn [x] (:name x))] 2)
                                (link 2 [:org :name] 3)
                                (link 1 [:org-domain :domain] 3))
                            (-> {}
                                (link 2 [:group :id] 4)))
                     3)
       (map (juxt first last))))

(defn traverse-label [link-graph source label]
  (get-in link-graph [source label label-index]))

(defn traverse-path [link-graph source path]
  (when (seq path)
    (reduce (fn [current-source label]
              (if-let [next-source (traverse-label link-graph current-source label)]
                next-source
                (reduced nil)))
            source
            path)))

(comment
  (let [link-graph (-> {}
                       (link 1 :user 2)
                       (link 2 :org 3)
                       (link 1 :org-domain 3)
                       (link 3 :saml 4))]
    [(traverse-path link-graph 1 [])
     (traverse-path link-graph 1 [:foo])
     (traverse-path link-graph 1 [:user])
     (traverse-path link-graph 1 [:user :org])
     (traverse-path link-graph 1 [:user :org :saml])
     (traverse-path link-graph 1 [:user :saml :org])
     (traverse-path link-graph 1 [:org-domain :saml])]))
