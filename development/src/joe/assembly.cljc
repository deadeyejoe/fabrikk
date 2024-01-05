(ns joe.assembly
  (:require [fabrikk.alpha.core :as fab]
            [fabrikk.assembly.interface :as assembly]))

(def workspace
  (fab/->factory ::workspace
                 {:template {:id random-uuid
                             :name "workspace"}}))

(def model
  (fab/->factory ::model
                 {:template {:id random-uuid
                             :name "model"
                             :workspace (fab/one workspace)}}))

(def field
  (fab/->factory ::field
                 {:template {:id random-uuid
                             :name "field"
                             :workspace (fab/one workspace)}}))

(def node
  (fab/->factory ::node
                 {:template {:id random-uuid
                             :name "field"
                             :workspace (fab/one workspace)}}))

(def edge
  (fab/->factory :edge
                 {:template {:id random-uuid
                             :name "field"
                             :source (fab/one node)
                             :target (fab/one node)
                             :workspace (fab/derive [:source :workspace])}}))

(fab/build edge {} {:output-as :build-order})



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Maybe re-use directives
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(assembly/->assembly {:workspace (fab/one workspace)
                      :model (fab/one model {:with {:workspace :workspace}})
                      :field (fab/many field 0 {})})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Maybe use something assembly specific
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn coordinate [& args])

(defn single [& args])
(defn multi [& args])

(assembly/->assembly {:workspace (single workspace)
                      :model (single model)
                      :field (multi field)})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Maybe treat it like an anonymous factory
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(assembly/assemble {:workspace workspace
                    :model model
                    :fields [field]})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; No this:
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn entity [key factory & {:as args}]
  (merge args
         {:cardinality :one
          :key key
          :factory factory}))

(defn collection [key factory & {:as args}]
  (merge args
         {:cardinality :many
          :key key
          :factory factory}))

(defn merge [assembly])

(defn nest [path assembly])

(def workspace-assembly
  (assembly/->assembly (entity :workspace workspace)
                       (entity :model model
                               :unify {:workspace [:workspace]})
                       (collection :fields field
                                   :default [{:with {:name "Field one"}}
                                             {:with {:name "Field two"}}]
                                   :unify {:workspace [:workspace]})))

(def graph-assembly
  (assembly/->assembly (merge workspace-assembly)
                       {:nodes {:cardinality :many
                                :factory node
                                :unify {[:workspace] [:workspace]}
                                :default []
                                :index-by :name}
                        :edges {:cardinality :many
                                :factory edge
                                :default []
                                :reference {:source [:nodes :name]
                                            :target [:nodes :name]}}}))

(let [{:keys [workspace model]
       [field-one field-two] :fields} (assembly/assemble workspace-assembly
                                                         :nodes [{:with {:name "alpha"}}
                                                                 {:with {:name "beta"}}]
                                                         :edges [{:with {:source "alpha"
                                                                         :target "beta"}}])])
