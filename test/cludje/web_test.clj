(ns cludje.web-test
  (:use midje.sweet
        cludje.test
        cludje.web))

(fact "ring-parser"
  (fact "adds :params"
    (fact "from query-string"
      (ring-parser {:query-string "a=1"}) => (contains {:params {:a "1"}}))
    ))
    ;(fact "from json"
      ;(ring-parser {:body {:form-params {:a 1}} :content-type "application/json"}) =>
        ;(contains {:params {:a "1"}}))))
