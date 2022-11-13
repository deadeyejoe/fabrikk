(ns extrude.utils.interface
  (:require [extrude.utils.core :as core]))

(defn scoped-assoc [& path]
  (apply core/scoped-assoc path))

(defn scoped-update [& path]
  (apply core/scoped-update path))
