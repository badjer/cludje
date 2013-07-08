(ns cludje.server-test
  (:use midje.sweet
        cludje.test
        cludje.core
        cludje.app
        cludje.renderer
        cludje.dispatcher
        cludje.server))

(def req {:url "http://localhost:8099"})

(facts "JettyServer"
  (let [serv (->JettyServer 8099 (atom identity) (atom nil))]
    (start- serv) => anything 
    (do-request req) => (contains {:status 200})
    (stop- serv) => anything
    (do-request) => (throws)))

(facts "JettyServer set-handler- works"
  (let [serv (->JettyServer 8099 (atom nil) (atom nil))]
    (set-handler- serv identity) => anything
    (start- serv) => anything
    (do-request req) => (contains {:status 200})
    (stop- serv) => anything))

(defaction ac-echo input)

(def json-req {:url "http://localhost:8099" :method :json :body {:a 1}})

(facts "JettyServer handles JSON with make-ring-handler"
  (let [serv (->JettyServer 8099 (atom nil) (atom nil))
        sys {:dispatcher (->Dispatcher (atom {:default ac-echo}))
             :renderer (->JsonRenderer)}
        handler (make-ring-handler sys)]
    (set-handler- serv handler) => anything
    (start- serv) => anything
    (do-request json-req) => {:a 1}
    (stop- serv) => anything))

(def res-request {:url "http://localhost:8099/css/test.css"})
(facts "JettyServer serves static files"
  (let [serv (->JettyServer 8099 (atom nil) (atom nil))
        sys {:dispatcher (->Dispatcher (atom {:default ac-echo}))
             :renderer (->JsonRenderer)}
        handler (make-ring-handler sys)]
    (set-handler- serv handler) => anything
    (start- serv) => anything
    (:body (do-request res-request)) => "hello world\n"
    (stop- serv) => anything))



