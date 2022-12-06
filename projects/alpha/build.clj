(ns build
  (:require [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]))

(def lib 'org.clojars.joe-douglas/fabrikk)
(def version (format "0.0.%s" (b/git-count-revs nil)))

(defn clean [_]
  (bb/clean))

(defn jar [opts]
  (-> opts
      (assoc :lib lib
             :version version
             :transitive true)
      (bb/clean)
      (bb/jar)))


(defn install "Install the JAR locally." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/install)))

(defn deploy "Deploy the JAR to Clojars."
  [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/deploy)))

(comment
  (jar {})
  (install {}))
