(ns cludje.dataadapt-test
  (:use midje.sweet
        cludje.system
        cludje.dataadapt
        cludje.test))

(def input {:a "1"})
(def context {:raw-input input})

(fact ">TestDataAdapter"
  (let [tda (>TestDataAdapter)]
    (fact "Parses raw input"
      (parse-input tda context) => (contains {:parsed-input input}))))

(def web-context {:raw-input {:query-string "a=1"}})

(fact ">WebDataAdapter"
  (let [wda (>WebDataAdapter)]
    (fact "Parses raw input"
      (parse-input wda web-context) => (contains {:parsed-input input}))))
