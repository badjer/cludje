(ns cludje.templates.angular-test
  (:use midje.sweet
        cludje.templates.angular))

(facts "->js"
  (->js 1) => 1
  (->js {:a 1}) => "{a: 1}"
  (->js {:a :b}) => "{a: 'b'}"
  (->js {:a 1 :b 2}) => "{a: 1, b: 2}"
  (->js {:a "abc"}) => "{a: 'abc'}"
  (->js {:a "abc" :b 1}) => "{a: 'abc', b: 1}"
  (->js {:a {:b 1} :c 2}) => "{a: {b: 1}, c: 2}")
