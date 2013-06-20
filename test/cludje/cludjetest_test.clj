(ns cludje.cludjetest-test
  (:use cludje.test
        midje.sweet))

(fact "has-keys"
  (let [x {:a 1 :b 2}]
    x => (has-keys :a :b)
    x => (has-keys :a)
    x => (has-keys :b)
    x =not=> (has-keys :c)
    x =not=> (has-keys :c :d)
    x =not=> (has-keys :a :c)
    x =not=> (has-keys :a :b :c)))

(fact "just-keys"
  (let [x {:a 1 :b 2}]
    x => (just-keys :a :b)
    x =not=> (just-keys :a)
    x =not=> (just-keys :a :b :c)
    x =not=> (just-keys :c)))

(fact "has-problems?"
  {} =not=> has-problems?
  {:problems {}} => has-problems?
  nil =not=> has-problems?
  {:a 1} =not=> has-problems?)

(fact "has-problems"
  {:problems {:a 1}} => (has-problems :a)
  {:problems {:a 1}} =not=> (has-problems :b)
  {:problems {}} =not=> (has-problems :a)
  {} =not=> (has-problems :a)
  {:problems {:a 1 :b 1}} => (has-problems :a :b)
  {:problems {:a 1}} => (has-problems)
  {:problems {}} => (has-problems)
  {} =not=> (has-problems)
  nil =not=> (has-problems))

(fact "do-request"
  (do-request {}) => (contains {:status 200}))

