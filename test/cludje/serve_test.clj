(ns cludje.serve-test
  (:use midje.sweet
        cludje.errors
        cludje.serve
        cludje.system
        cludje.application
        cludje.pipeline
        cludje.model
        cludje.mold
        cludje.types
        cludje.test))

(defn params-handler [{:keys [params] :as request}]
  (assoc request :result params))

(defn session-handler [{:keys [session] :as request}]
  (assoc request :result session))

(defn inc-cog [{:keys [params session] :as request}]
  (let [res (-> request
                (assoc :result {:ses session})
                (assoc :output {:ses session}))
        a (get session :a 1)
        inc-a (+ a 1)]
    ; If the input contains inc, inc the session var
    (if (:inc params)
      (assoc-in res [:session :a] inc-a)
      res)))
      

(def session {:a 1})
(def input {:b 1})


(defn new-widget [request] {:output {:price 100} :result {:price "$1.00"}})


(def req {:url "http://localhost:8099"})
(def jetty-config {:port 8099})

(def result {:a 1})
(defn static-handler [request] (assoc request :result result))

(facts ">JettyServer"
  (let [serv (>JettyServer static-handler)]
    (start serv jetty-config) => anything 
    (do-request req) => (contains {:status 200})
    (stop serv) => anything
    (do-request) => (throws)))

(def cog (>Model {:name Str :ses Anything} {:modelname "cog"}))
(defn new-cog [request] (assoc request :output {:name "A"}))
(defn problem-cog [_] (throw-problems {:name "empty"}))
(defn unauthorized-cog [_] (throw-unauthorized))
(defn notloggedin-cog [_] (throw-not-logged-in))


(defn >json-req 
  ([action] (>json-req action nil))
  ([action input] (>json-req action input nil))
  ([action input cookies]
    {:url "http://localhost:8099/api" 
     :method :json
     :cookies cookies
     :body (merge input {:_action (name action)})}))

(def json-req (>json-req :new-cog {:name "B"}))
(def get-req {:method :get-json 
              :url "http://localhost:8099/api?name=B&_action=new-cog"})

(def sys-config {:action-namespaces ['cludje.serve-test]
                 :mold-namespaces ['cludje.serve-test]})

(facts ">JettyServer"
  (let [sys (>test-system sys-config)
        pipeline (-> api-pipeline (in-system sys))
        server (>JettyServer pipeline)]
    (start server jetty-config) => anything
    (fact "handles GET request"
      (do-request get-req) => (body {:name "A"}))
    (fact "handles JSON input"
      (do-request json-req) => (body {:name "A"}))
    (fact "handles exceptions"
      (do-request (>json-req :problem-cog)) => (status 200)
      (:body (do-request (>json-req :problem-cog))) => 
        (contains {:__problems {:name "empty"}})
      (do-request (>json-req :unauthorized-cog)) => (status 403)
      (do-request (>json-req :notloggedin-cog)) => (status 401))
    (fact "Persists session"
      (let [cs (>cookies)]
        (do-request (>json-req :inc-cog {} cs)) => (body {:ses {}})
        (do-request (>json-req :inc-cog {:inc true} cs)) => (status 200)
        (do-request (>json-req :inc-cog {} cs)) => (body {:ses {:a 2}})
      ))
    (fact "Resetting clears session")
    (stop server) => anything))

