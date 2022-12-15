(ns fabrikk.build-graph.interface-test
  (:require [fabrikk.build-graph.core :as value-graph]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.set :as set]
            [clojure.test :as test :refer :all]
            [loom.graph :as graph]
            [loom.io :as lio]))

(defn edges->graph [edges]
  (let [nodes (set (flatten (vec edges)))
        with-nodes (reduce (fn [g node]
                             (value-graph/add-node g node node))
                           (value-graph/build)
                           nodes)]
    (reduce (fn [g [source target :as edge]]
              (value-graph/add-edge g source target edge))
            with-nodes
            edges)))

(comment
  (edges->graph [])
  (edges->graph [[1 2]])
  (lio/view (edges->graph [[1 2] [1 3] [2 3]])))

(def empty-graph (value-graph/build))

(deftest basic-loom-tests
  (let [value-graph (edges->graph [[1 2] [1 3] [2 3] [1 {:foo :bar}]])]
    (is (graph/has-node? value-graph 1))
    (is (graph/has-node? value-graph {:foo :bar}) "Keys are arbitrary")
    (is (not (graph/has-node? value-graph 4)))
    (is (graph/has-edge? value-graph 1 2))
    (is (graph/has-edge? value-graph 1 {:foo :bar}))
    (is (not (graph/has-edge? value-graph 1 4)))
    (is (not (graph/has-edge? value-graph 2 1)) "Graph is directed")
    (is (= #{[1 2] [1 3] [1 {:foo :bar}]}
           (set (graph/out-edges value-graph 1))))))

(deftest test-add-node
  (is (thrown? AssertionError (value-graph/add-node (edges->graph [[1 2]]) 1 {:foo 1})))
  (let [node-only (value-graph/add-node (value-graph/build) 1 {:foo 1})]
    (is (graph/has-node? node-only 1))
    (is (= 0 (count (graph/edges node-only))))
    (is (= {:foo 1} (value-graph/node-value node-only 1)))))

(deftest test-set-node
  (is (thrown? AssertionError (value-graph/set-node empty-graph 1 {:foo :bar})))
  (let [node-only (value-graph/add-node (value-graph/build) 1 {:foo 1})]
    (is (= :foo (-> (value-graph/set-node node-only 1 :foo)
                    (value-graph/node-value 1))))))

(deftest test-update-node
  (is (thrown? AssertionError (value-graph/update-node empty-graph 1 inc [])))
  (let [node-only (value-graph/add-node (value-graph/build) 1 1)]
    (is (= 2 (-> (value-graph/update-node node-only 1 inc [])
                 (value-graph/node-value 1))))))

(deftest test-add-edge
  (is (thrown? AssertionError (value-graph/add-edge (edges->graph [[1 2] [1 3]]) :X 2 {:foo 1}))
      "source must exist already")
  (is (thrown? AssertionError (value-graph/add-edge (edges->graph [[1 2] [1 3]]) 1 :X {:foo 1}))
      "target must exist already")
  (is (thrown? AssertionError (value-graph/add-edge (edges->graph [[1 2] [1 3]]) 1 2 {:foo 1})))
  (let [with-added-edge (value-graph/add-edge (edges->graph [[1 2] [3 4]]) 2 3 {:foo 1})]
    (is (graph/has-edge? with-added-edge 2 3))
    (is (= {:foo 1} (value-graph/edge-value with-added-edge 2 3)))))

(deftest test-set-edge
  (is (thrown? AssertionError (value-graph/set-edge (edges->graph [[1 2]]) 3 4 :X)))
  (is (= :foo (-> (value-graph/set-edge (edges->graph [[1 2]]) 1 2 :foo)
                  (value-graph/edge-value 1 2)))))

(deftest test-update-edge
  (is (= {0 1} (-> (value-graph/update-edge (edges->graph [[1 2]]) 3 4 update [0 (fnil inc 0)])
                   (value-graph/edge-value 3 4))))
  (is (= [2 2] (-> (value-graph/update-edge (edges->graph [[1 2]]) 1 2 update [0 inc])
                   (value-graph/edge-value 1 2)))))

(deftest self-loop
  (let [loop-graph (-> (value-graph/build)
                       (value-graph/add-node 1 nil)
                       (value-graph/add-edge 1 1 nil))]
    (is (graph/has-node? loop-graph 1))
    (is (graph/has-edge? loop-graph 1 1))))

(s/def ::node (set (range 1 11)))
(s/def ::edge (s/coll-of ::node :kind vector? :count 2))
(s/def ::edge-set (s/coll-of ::edge :kind set?))
(comment
  (s/valid? ::edge [1])
  (gen/generate (s/gen ::edge-set)))

(def combine-existing (some-fn identity))
(defn combine-new [x y] y)

(deftest test-merge-combine
  (let [merged (value-graph/merge (edges->graph [[1 2]])
                                  (-> (value-graph/build)
                                      (value-graph/add-node 1 {:node 1})
                                      (value-graph/add-node 2 {:node 2})
                                      (value-graph/add-edge 1 2 {:edge [1 2]}))
                                  combine-new
                                  combine-new)]
    (is (= {:node 1} (value-graph/node-value merged 1)))
    (is (= {:edge [1 2]} (value-graph/edge-value merged 1 2)))))

(deftest test-merge-generated
  (dotimes [n 1000]
    (let [edge-set-1 (gen/generate (s/gen ::edge-set))
          edge-set-2 (gen/generate (s/gen ::edge-set))
          merged (set/union edge-set-1 edge-set-2)]
      (is (= (edges->graph merged)
             (value-graph/merge (edges->graph edge-set-1)
                                (edges->graph edge-set-2)
                                combine-existing
                                combine-existing))))))

(def dag
  (edges->graph [[1 2] [1 3] [1 4] [1 7]
                 [2 4] [2 5]
                 [4 5] [4 6]]))
(def one-cycle
  (edges->graph [[1 1]]))
(def two-cycle
  (edges->graph [[1 2] [2 1]]))
(def three-cycle
  (edges->graph [[1 2] [2 3] [3 1]]))
(def bicycle
  (edges->graph [[1 2] [2 1] [3 2] [3 4] [4 3]]))
(def three-cycle-with-stalk
  (edges->graph [[1 2] [2 3] [3 1] [4 1]]))
(defn lonely-node [n]
  (-> (value-graph/build)
      (value-graph/add-node n n)))

(comment
  (lio/view dag)
  (lio/view bicycle))

(deftest test-successor-graph
  (testing "Cycle"
    (is (= one-cycle
           (value-graph/successor-graph one-cycle 1)))
    (is (= two-cycle
           (value-graph/successor-graph two-cycle 1)
           (value-graph/successor-graph two-cycle 2)))
    (is (= three-cycle
           (value-graph/successor-graph three-cycle 1)
           (value-graph/successor-graph three-cycle 2)))
    (is (= bicycle
           (value-graph/successor-graph bicycle 3)))
    (is (= two-cycle
           (value-graph/successor-graph bicycle 2)))
    (is (= three-cycle
           (value-graph/successor-graph three-cycle-with-stalk 1))))
  (testing "Dag"
    (is (= dag (value-graph/successor-graph dag 1)))
    (is (= (lonely-node 1)
           (value-graph/successor-graph (lonely-node 1) 1)))
    (is (= (lonely-node 7)
           (value-graph/successor-graph dag 7)))
    (is (= (edges->graph [[4 5] [4 6]])
           (value-graph/successor-graph dag 4)))
    (is (= (edges->graph [[2 4] [2 5] [4 5] [4 6]])
           (value-graph/successor-graph dag 2)))))
