(ns fabrikk.build-graph.label-graph
  (:refer-clojure :exclude [merge]))

(defn link [lg source label target]
  (assoc-in lg [source label] target))

(defn conflict? [lg source label target]
  (= (get-in lg [source label]) target))

(def no-conflict? (complement conflict?))

(defn link-no-conflict! [lg source label target]
  (assert (no-conflict? lg source label target))
  (link lg source label target))

(defn merge [graph other-graph]
  (merge-with clojure.core/merge graph other-graph))

(defn in-edges [lg node]
  (mapcat (fn [[source label->dest]]
            (keep (fn [[label dest]]
                    (when (= node dest)
                      [source label dest]))
                  label->dest))
          lg))

(comment
  (link {} 1 :org 2)
  (link {} 1 :org 1)
  (link-no-conflict! {1 {:org 2}} 1 :org 2)
  (->> (in-edges (merge (-> {}
                            (link 1 :user 2)
                            (link 2 :org 3)
                            (link 1 :org-domain 3))
                        (-> {}
                            (link 2 :group 4)))
                 3)
       (map (juxt first last)))
  )

(defn traverse-label [lg source label]
  (get-in lg [source label]))

(defn traverse-path [lg source path]
  (when (seq path)
    (reduce (fn [current-source label]
              (if-let [next-source (traverse-label lg current-source label)]
                next-source
                (reduced nil)))
            source
            path)))

(comment
  (let [lg (-> {}
               (link 1 :user 2)
               (link 2 :org 3)
               (link 1 :org-domain 3)
               (link 3 :saml 4))]
    [(traverse-path lg 1 [])
     (traverse-path lg 1 [:foo])
     (traverse-path lg 1 [:user])
     (traverse-path lg 1 [:user :org])
     (traverse-path lg 1 [:user :org :saml])
     (traverse-path lg 1 [:user :saml :org])
     (traverse-path lg 1 [:org-domain :saml])]))
