(ns joe
  (:require [clojure.spec.alpha :as s]
            [orchestra.spec.test :as ost]
            [clojure.tools.namespace.repl :refer [refresh]]
            [expound.alpha :as expound]
            [portal.api :as p]))

(ost/instrument)
(set! s/*explain-out* expound/printer)
(defn open-portal []
  (let [p (p/open {:launcher :vs-code})]
    (add-tap #'p/submit)
    (tap> :TESTING)
    p))
(comment
  (open-portal)) 
(refresh)
