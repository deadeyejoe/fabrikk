(ns fabrikk.utils.core)

(defn scoped-assoc [& path]
  (let [pathv (vec path)]
    (fn [m k v]
      (assoc-in m (conj pathv k) v))))

(defn scoped-update [& path]
  (let [pathv (vec path)]
    (fn [m k f & args]
      (apply update-in m (conj pathv k) f args))))

(comment
  ((scoped-assoc :value) {} :foo :bar)
  ((scoped-update :value) {} :foo (fnil inc 0)))

(defn copy-docs 
  "Copies docs from one function to another.
   
   Arguments must be vars e.g. `(copy-docs #'from-fn #'to-fn)`"
  [sym other]
  (alter-meta! sym assoc :doc (:doc (meta other))))

(defn pad-with-last
  "Create a collection of 'n' elements based on 'coll'.
   
   If the number of items in coll is less than n, repeat the last
   element in coll to make up the remainder. If the number of items
   is less than n, behaves like `clojure.core/take`."
  [n coll]
  (let [last-element (last coll)]
    (->> (concat coll (repeat last-element))
         (take n))))

(comment
  (pad-with-last -1 [1 2 3])
  (pad-with-last 1 [1 2 3])
  (pad-with-last 2 [1 2 3])
  (pad-with-last 3 [1 2 3])
  (pad-with-last 10 [1 2 3])
  (pad-with-last 3 [{}])
  (pad-with-last 3 []))
