(ns cludje.server-test
  (:use midje.sweet
        cludje.test
        cludje.core
        cludje.app
        cludje.server))

(def req {:url "http://localhost:8099"})

(facts "JettyServer"
  (let [serv (->JettyServer 8099 (atom identity) (atom nil))]
    (start serv) => anything 
    (do-request req) => (contains {:status 200})
    (stop serv) => anything
    (do-request) => (throws)))

(facts "JettyServer set-handler works"
  (let [serv (->JettyServer 8099 (atom nil) (atom nil))]
    (set-handler serv identity) => anything
    (start serv) => anything
    (do-request req) => (contains {:status 200})
    (stop serv) => anything))

