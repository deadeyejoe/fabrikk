(ns fabrikk.utils.interface
  (:require [fabrikk.utils.core :as core]))

(defn tapout> [x label]
  (core/tapout> x label))

(defn tapout>> [label x]
  (core/tapout>> label x))

(defn scoped-assoc [& path]
  (apply core/scoped-assoc path))

(defn scoped-update [& path]
  (apply core/scoped-update path))

(defn copy-docs [sym other]
  (core/copy-docs sym other))
;; SORCERY! >:-|
(core/copy-docs #'copy-docs #'core/copy-docs)

(defn pad-with-last [n coll]
  (core/pad-with-last n coll))
(core/copy-docs #'pad-with-last #'core/pad-with-last)
