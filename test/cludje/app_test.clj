(ns cludje.app-test
  (:use midje.sweet
        cludje.test
        cludje.core
        cludje.dispatcher
        cludje.app))

(facts "make-system initializes all subsystems"
  (make-system) => (has-keys :db :mailer :logger :auth :dispatcher
                             :renderer :server))

(facts "make-system allows overriding subsystems"
  (let [dispatcher (->Dispatcher (atom {}))
        sysoverrides {:dispatcher dispatcher}
        sys (make-system sysoverrides)]
    (:dispatcher sys) => dispatcher))

(def req {:url "http://localhost:8888"})

(fact "a start-system -ed system can respond to requests"
  (let [sys (make-system)]
    (start-system sys) => anything
    ; Now that the system is started, we should be able to connect to it 
    (do-request req) => (contains {:status 200})
    (do-request req) => (contains {:body "hello world"})
    (fact "stopping the system stops the webserver"
      (stop-system sys) => anything
      (do-request req) => (throws))))


(defaction ac-add1 
  ; Add 1 to items
  (write :items nil {:a 1})
  ; Return the count of items
  {:body (str (count (query :items nil)))})

(facts "app db survives restart"
  (let [dispatches {:default ac-add1}
        dispatcher (->Dispatcher (atom {:default ac-add1}))
        sysoverrides {:dispatcher dispatcher}
        sys (make-system sysoverrides)]
    (:dispatcher sys) => dispatcher
    (start-system sys) => anything
    (do-request req) => (contains {:body "1"})
    (do-request req) => (contains {:body "2"})
    (stop-system sys) => anything
    (start-system sys) => anything
    (do-request req) => (contains {:body "3"})
    (stop-system sys) => anything))
