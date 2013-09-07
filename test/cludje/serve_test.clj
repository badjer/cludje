(ns cludje.serve-test
  (:use midje.sweet
        cludje.errors
        cludje.serve
        cludje.system
        cludje.application
        cludje.model
        cludje.types
        cludje.test))

(def req {:url "http://localhost:8099"})

(facts ">JettyServer"
  (let [serv (>JettyServer)]
    (start serv 8099 identity) => anything 
    (do-request req) => (contains {:status 200})
    (stop serv) => anything
    (do-request) => (throws)))

(def cog (>Model {:name Str} {:modelname "cog"}))
(defn new-cog [{:keys [input]}] {:name "A"})
(defn problem-cog [_] (throw-problems {:name "empty"}))
(defn unauthorized-cog [_] (throw-unauthorized))
(defn notloggedin-cog [_] (throw-not-logged-in))

(defn >json-req 
  ([action] (>json-req action nil))
  ([action input] 
  {:url "http://localhost:8099/api" :method :json
   :body (merge input {:_action (name action)})}))

(def json-req (>json-req :new-cog {:name "B"}))
(def get-req {:method :get-json 
              :url "http://localhost:8099/api?name=B&_action=new-cog"})

(def sys-config {:action-namespaces ['cludje.serve-test]
                 :mold-namespaces ['cludje.serve-test]})


(facts ">JettyServer"
  (let [sys (with-web (>test-system sys-config))
        handler (>api-pipeline sys)
        server (>JettyServer)]
    (start server 8099 handler) => anything
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
    (stop server) => anything))
