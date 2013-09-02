(ns cludje.find-test
  (:use midje.sweet
        cludje.test
        cludje.find))

(def x 1)

(fact "find-in-ns"
  (fact "finds"
    (fact "with string"
      (find-in-ns 'cludje.find-test "x") => `x)
    (fact "with keyword"
      (find-in-ns 'cludje.find-test :x) => `x)
    (fact "with symbol"
      (find-in-ns 'cludje.find-test 'x) => `x))
  (fact "works with fully qualified name"
    (find-in-ns 'cludje.find-test "cludje.find-test/x") => `x)
  (fact "returns nil if not found"
    (find-in-ns 'cludje.find-test "y") => nil))


