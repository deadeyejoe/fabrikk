(ns joe
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.namespace.repl :refer [refresh]]
            [expound.alpha :as expound]
            [portal.api :as p]))

(set! s/*explain-out* expound/printer)
(defn open-portal []
  (let [p (p/open {:launcher :vs-code})]
    (add-tap #'p/submit)
    (tap> :TESTING)
    p))
(comment
  (open-portal)) 
(refresh)
