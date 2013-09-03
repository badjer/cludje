(ns cludje.dataadapt-test
  (:use midje.sweet
        cludje.system
        cludje.dataadapt
        cludje.test))

(fact ">TestDataAdapter"
  (let [tda (>TestDataAdapter)]
    (fact "Sets action"
      (parse-input tda {:_action "foo"}) => (contains {:_action "foo"}))))

(fact ">WebDataAdapter"
  (let [wda (>WebDataAdapter)]
    (fact "Sets action"
      (parse-input wda {:query-string "_action=foo"}) => (contains {:_action "foo"}))))

