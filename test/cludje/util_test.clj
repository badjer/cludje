(ns cludje.util-test
  (:use midje.sweet
        cludje.util))

(facts "map-vals"
  (map-vals {:a 1 :b 1} inc) => {:a 2 :b 2})

(defn getx [] "x")

(fact "realize"
  (fact "with val"
    (realize 1) => 1)
  (fact "with fn"
    (realize getx) => "x")
  (fact "within map-vals"
    (map-vals {:a getx :b 1} realize) => {:a "x" :b 1}))
