(ns cludje.parser-test
  (:use midje.sweet
        cludje.core
        cludje.parser))

(fact "WebInputParser"
  (let [p (->WebInputParser true)]
    (parse-input- p {:uri "/api" :params {:a 1}}) => {:a 1}
    (fact "Returns nil if it's the wrong uri"
      (parse-input- p {:uri "/foobar" :params {:a 1}}) => nil)
    (fact "Strips off any fields in input that start with __"
      (parse-input- p {:uri "/api" :params {:__alerts ["a"] :a 1}}) => {:a 1})))
