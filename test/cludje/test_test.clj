(ns cludje.test-test
  (:use cludje.test 
        cludje.types
        cludje.model
        cludje.errors
        cludje.pipeline
        cludje.mold
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


(fact ">request"
  (>request {:a 1}) => {:input {:a 1}}
  (>request {} {:user :me}) => {:user :me :input {}}
  (fact "first parameter input always takes precedence"
    (>request {:a 1} (with-input {:a 2})) => {:input {:a 1}})
  )


(defn action [request]
  (:input request))

(defn action-response [request]
  (with-output request (:input request)))

(fact "run"
  (run action {:a 1}) => {:a 1}
  (run action {:a 1} (with-input {:a 2})) => {:a 1}
  (run action {:a 1} (as-user :foo)) => {:a 1}
  (fact "with an action that returns a response object"
    (run action-response {:a 1}) => {:a 1})
  )

(defn user-action [request]
  (:user request))

(fact "contextualize-run"
  (let [run-as-user (contextualize-run (as-user {:name :foo}))]
    (run-as-user user-action {}) => {:name :foo}
    (fact "context can be explicitly overridden"
      (run-as-user user-action {} (as-user {:name :bar})) => {:name :bar})
    ))

(def mold (>Mold {:a Str} {}))

(fact "render"
  (fact "uses mold to format output"
    (render action {:a 1} (with-output-mold mold)) => {:a "1"})
  (fact "no mold uses anything as mold"
    (render action {:a 1}) => {:a 1})
  (fact "with an action that returns a response object"
    (render action-response {:a 1}) => {:a 1}
    (render action-response {:a 1} (with-output-mold mold)) => {:a "1"})
  )

(fact "contextualize-render"
  (let [render-as-user (contextualize-render (as-user {:name :foo}))]
    (render-as-user user-action {}) => {:name :foo}
    (fact "context can be explicitly overridden"
      (render-as-user user-action {} (as-user {:name :bar})) => {:name :bar})
    ))
