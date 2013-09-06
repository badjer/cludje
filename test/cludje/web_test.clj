(ns cludje.web-test
  (:use midje.sweet
        cludje.test
        cludje.web))

(fact "ring-parser"
  (fact "adds :params"
    (fact "from query-string"
      (ring-parser {:query-string "a=1"}) => (contains {:params {:a "1"}}))
    ))
