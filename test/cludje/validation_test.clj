(ns cludje.validation-test
  (:use midje.sweet
        cludje.test
        cludje.validation))

(fact "value?"
  (value? nil) => falsey
  (value? "") => falsey
  (value? "a") => truthy
  (value? 1) => truthy)

(fact "needs should return an entry for each missing field"
  (needs {} :a) => (has-keys :a)
  (needs {} :a :b) => (has-keys :a :b)
  (needs {:a 1} :a) => falsey
  (needs {:a ""} :a) => (has-keys :a)
  (needs {:a nil} :a) => (has-keys :a)
  (needs {:a 1} :b) => (has-keys :b)
  (needs {:a 1 :b 2} :a :b) => falsey)

(fact "email?"
  (email? "") => falsey
  (email? "a") => falsey
  (email? "a@b.cd") => truthy)

(fact "min-length?"
  (min-length? "" 3) => falsey
  (min-length? "asdf" 3) => truthy
  (min-length? "asd" 3) => truthy)

(fact "bad should return truthy only if the value? and not the given test"
  (bad email? nil) => falsey
  (bad email? "") => falsey
  (bad email? "a") => truthy
  (bad email? "a@b.cd") => falsey)

(fact "no-bad should return an entry for each field that 
      isn't null and is invlid"
  (no-bad email? {} :a) => falsey
  (no-bad email? {:a ""} :a) => falsey
  (no-bad email? {:a "a"} :a) => (has-keys :a)
  (no-bad email? {:a "a@b.cd"} :a) => falsey)

