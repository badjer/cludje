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

(fact "has-alerts?"
  {} =not=> has-alerts?
  {:__alerts []} => has-alerts?
  nil =not=> has-alerts?
  {:a 1} =not=> has-alerts?)

(fact "has-alert"
  {} =not=> (has-alert :success #"saved")
  {:__alerts [{:type :success :text "save"}]} => (has-alert :success #"save")
  {:__alerts [{:type :info :text "save"}]} =not=> (has-alert :success #"save")
  {:__alerts [{:type :success :text ""}]} =not=> (has-alert :success #"save")
  {:__alerts [{:text ""}]} =not=> (has-alert :success #"save")
  {:__alerts [{:type :success}]} =not=> (has-alert :success #"save")
  nil =not=> (has-alert :success #"save")
  {:a 1} =not=> (has-alert :success #"save")
  (fact "works on a seq of alerts"
    {:__alerts '({:type :success :text "save"} {:type :info :text "s"})}
      => (has-alert :success #"save")))

(fact "exception checkers"
  (fact "404"
    (throw-not-found) => (throws-404)
    (throw-unauthorized) =not=> (throws-404)
    (throw-not-logged-in) =not=> (throws-404)
    (+ 1 2) =not=> (throws-404))
  (fact "403"
    (throw-not-found) =not=> (throws-403)
    (throw-unauthorized) => (throws-403)
    (throw-not-logged-in) =not=> (throws-403)
    (+ 1 2) =not=> (throws-403))
  (fact "401"
    (throw-not-found) =not=> (throws-401)
    (throw-unauthorized) =not=> (throws-401)
    (throw-not-logged-in) => (throws-401)
    (+ 1 2) =not=> (throws-401)))

(fact "ok?"
  {:a 1} => ok?
  nil => ok?
  {} => ok?
  "" => ok?
  true => ok?
  (throw-not-found) =not=> ok?
  (throw-unauthorized) =not=> ok?
  (throw-not-logged-in) =not=> ok?
  {:__problems {}} =not=> ok?)

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

