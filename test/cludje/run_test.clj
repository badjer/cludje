(ns cludje.run-test
  (:use midje.sweet
        cludje.errors
        cludje.run))

(def output {:price 987})
(defn action [request] (assoc request :output output))
(defn just-output-action [request] output)
(defn problem-action [request] (throw-problems {:name "bad"}))
(defn error-action [request] (throw-error))

(fact "run-action"
  (fact "when action returns response map"
    (let [response (run-action action {:system {}})]
      (fact "sets the output"
        response => (contains {:output output}))
      (fact "returns the request map"
        response => (contains {:system {}}))))
  (fact "when action only returns output"
    (let [response (run-action just-output-action {:system {}})]
      (fact "sets the output"
        response => (contains {:output output}))
      (fact "returns the request map"
        response => (contains {:system {}}))))
  (fact "catch problems and merge them into the response map"
    (run-action problem-action {}) => {:output {:__problems {:name "bad"}}})
  (fact "any non-problem exception is propagated"
    (run-action error-action {}) => (throws)))




