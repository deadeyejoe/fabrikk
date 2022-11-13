(ns extrude.directive.core
  (:require [clojure.spec.alpha :as s]
            [medley.core :as medley]))

(s/def ::type qualified-keyword?)
(s/def ::value any?)
(s/def ::directive (s/keys :req [::type]
                           :opt-un [::value]))

(def directive-type-kw ::type)

(defn ->directive
  ([type-kw] (->directive type-kw {}))
  ([type-kw opts]
   (merge {directive-type-kw type-kw} opts)))
(s/fdef ->directive
  :args (s/cat :type-kw qualified-keyword? :opts (s/? map?))
  :ret ::directive)

(defn directive? [m]
  (contains? m directive-type-kw))

(defn run-directive-dispatch [_ _ directive]
  (get directive-type-kw directive))
(defmulti run #'run-directive-dispatch)
(defmethod run :default [build-context key directive]
  ; context ns manipulates a build graph initially
  (context/assoc-value build-context key
                       (if (fn? directive)
                         (directive)
                         directive)))

(defn evaluate
  "Run directive, applying before/after hooks if present"
  [{:keys [::before ::after] :as directive}]
  (cond-> directive
    before (before)
    :always (run)
    after (after)))


;; =========== hooks ============

(defn before
  "Specify a function that runs before a directive. 
   Takes the directive as an argument and may return a modified directive."
  [directive f]
  (assoc directive ::before f))

(def before-hook ::before)

(defn after
  "Specify a function that runs after a directive
   Takes the result of the directive as an argument, and may return a modified result"
  [directive f]
  (assoc directive ::after f))

(def after-hook ::after)

;; =========== UTILS ===========

(defn run-directives [m]
  (medley/map-vals run m))

(defn evaluate-directives [m]
  (medley/map-vals evaluate m))
