(ns cludje.validate-test
  (:use midje.sweet
        cludje.test
        cludje.types
        cludje.validate))

(fact "needs should return an entry for each missing field"
  (needs {} :a) => (has-keys :a)
  (needs {} :a :b) => (has-keys :a :b)
  (needs {:a 1} :a) => falsey
  (needs {:a ""} :a) => (has-keys :a)
  (needs {:a nil} :a) => (has-keys :a)
  (needs {:a 1} :b) => (has-keys :b)
  (needs {:a 1 :b 2} :a :b) => falsey)

(fact "bad should work with a cludje type"
  (bad Email "a") => truthy
  (bad Email "a@b.cd") => falsey)

(fact "bad should work with a fn"
  (bad even? 1) => truthy
  (bad even? 2) => falsey)

(fact "bad should return truthy only if the value? and not the given test"
  (bad Email nil) => falsey
  (bad Email "") => falsey
  (bad Email "a") => truthy
  (bad Email "a@b.cd") => falsey)

(fact "no-bad should return an entry for each field that 
      isn't null and is invlid"
  (no-bad Email {} :a) => falsey
  (no-bad Email {:a ""} :a) => falsey
  (no-bad Email {:a "a"} :a) => (has-keys :a)
  (no-bad Email {:a "a@b.cd"} :a) => falsey)
