(ns cludje.parser
  (:use midje.sweet
        cludje.core
        cludje.web))

(fact "WebInputParser"
  (let [p (->WebInputParser true)]
    (parse-input- p {:uri "/api" :params {:a 1}}) => {:a 1}
    (fact "Returns nil if it's the wrong uri"
    (parse-input- p {:uri "/foobar" :params {:a 1}}) => nil)))
