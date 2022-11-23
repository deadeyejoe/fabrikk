(ns fabrikk.build-graph.beta-test
  (:require [fabrikk.build-graph.beta.core :as build-graph]
            [clojure.set :as set]
            [clojure.test :as test :refer :all]
            [loom.graph :as graph]
            [loom.io :as lio]))

(deftest test-merge
  (let [graph-1 (build-graph/init [1 2] [2 3])
        graph-2 (build-graph/init [1 2] ["a" 2] [2 "b"])

        merged (build-graph/merge graph-1 graph-2)]
    (is (= #{1 2 3 "a" "b"} (set (graph/nodes merged))))
    (is (= {[1 2] 2
            [2 3] 1
            ["a" 2] 1
            [2 "b"] 1}
           (frequencies (graph/edges merged))))
    (is (= (set/union (set (build-graph/edge-ids graph-1))
                      (set (build-graph/edge-ids graph-2)))
           (set (build-graph/edge-ids merged))))))

(def large-graph
  (build-graph/init [1 2] [1 4]
                    [2 3] [2 6] [2 8]
                    [3 4] [3 5]
                    [5 4]
                    [6 7] [6 7]
                    [8 8]))

(comment (lio/view large-graph))

(deftest test-subgraph
  (let [subgraph (build-graph/subgraph large-graph 3)]
    (is (= #{3 4 5} (graph/nodes subgraph)))
    (is (= 3 (count (graph/edges subgraph)))))
  (testing "Multi edges"
    (let [subgraph (build-graph/subgraph large-graph 6)]
      (is (= #{6 7} (graph/nodes subgraph)))
      (is (= [[6 7] [6 7]] (graph/edges subgraph)))))
  (testing "Self loops"
    (let [subgraph (build-graph/subgraph large-graph 8)]
      (is (= #{8} (graph/nodes subgraph)))
      (is (= [[8 8]] (graph/edges subgraph))))))
