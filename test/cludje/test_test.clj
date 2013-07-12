(ns cludje.test-test
  (:use cludje.test 
        cludje.core
        cludje.types
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
  {:__problems {}} => has-problems?
  nil =not=> has-problems?
  {:a 1} =not=> has-problems?)

(fact "has-problems"
  {:__problems {:a 1}} => (has-problems :a)
  {:__problems {:a 1}} =not=> (has-problems :b)
  {:__problems {}} =not=> (has-problems :a)
  {} =not=> (has-problems :a)
  {:__problems {:a 1 :b 1}} => (has-problems :a :b)
  {:__problems {:a 1}} => (has-problems)
  {:__problems {}} => (has-problems)
  {} =not=> (has-problems)
  nil =not=> (has-problems))

(fact "->json"
  (->json {:a 1}) => "{\"a\":1}")

(fact "<-json"
  (<-json "{\"a\":1}") => {:a 1})

(defmodel Cog {:amt Int})

(defaction ac-cog
  ; Save a new cog and then return the list of all cogs
  (save Cog input)
  (query Cog nil))

(fact "test-system works for crud"
  (let [sys (test-system)]
    (count (ac-cog sys {:amt 1})) => 1
    (count (ac-cog sys {:amt 1})) => 2))
