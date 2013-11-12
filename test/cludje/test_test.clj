(ns cludje.test-test
  (:use cludje.test 
        cludje.types
        cludje.model
        cludje.errors
        cludje.pipeline
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

(facts "has-item?"
  [{:a 1}] => (has-item? {:a 1})
  [{:a 1} {:z 1}] => (has-item? {:a 1})
  [{:a 1 :b 1}] => (has-item? {:a 1})
  1 =not=> (has-item? {:a 1})
  nil =not=> (has-item? {:a 1})
  [] =not=> (has-item? {:a 1})
  [{:z 1}] =not=> (has-item? {:a 1})
  [{:a 2}] =not=> (has-item? {:a 1}))

(facts "just-item?"
  [{:a 1}] => (just-item? {:a 1})
  [{:a 1 :b 1}] => (just-item? {:a 1})
  '({:a 1 :b 1}) => (just-item? {:a 1})
  (seq [{:a 1}]) => (just-item? {:a 1})
  [{:a 1} {:z 1}] =not=> (just-item? {:a 1})
  1 =not=> (just-item? {:a 1})
  nil =not=> (just-item? {:a 1})
  [] =not=> (just-item? {:a 1})
  [{:z 1}] =not=> (just-item? {:a 1})
  [{:a 2}] =not=> (just-item? {:a 1}))


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
  (fact "problems"
    (throw-problems {}) => (throws-problems)
    (throw-error {}) =not=> (throws-problems))
  (fact "error"
    (throw-error {}) => (throws-error)
    (throw-problems {}) =not=> (throws-error))
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

(fact "body"
  {:body {:a 1}} => (body {:a 1})
  {:body {:a 1}} =not=> (body {:b 1})
  {} =not=> (body {:a 1}))

(fact "status"
  {:status 200} => (status 200)
  {:status 400} =not=> (status 200)
  {} =not=> (status 200))


(defn action [request]
  (:input request))

(defn set-session [request]
  (let [new-val (get-in request [:input :a])]
    (-> request
        (assoc :output {}) 
        (assoc-in [:session :a] new-val))))

(defn get-session [request]
  {:a (get-in request [:session :a])})

(fact "run"
  (let [system (>test-system {})
        in-sys (in-system action-pipeline system)
        session (atom {})
        in-sess (in-session in-sys session)]
    (fact "in-system"
      (run in-sys action {:a 1}) => {:a 1})
    (fact "in-session"
      (run in-sess action {:a 1}) => {:a 1}
      (fact "persists session"
        (run in-sess get-session {}) => {:a nil}
        @session => {}
        (run in-sess set-session {:a 2}) => anything
        @session => {:a 2}
        (run in-sess get-session {}) => {:a 2}
      ))))

