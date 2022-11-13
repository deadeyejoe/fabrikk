(ns extrude.utils.core)

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
