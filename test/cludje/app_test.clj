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

(fact "a start-system -ed system can respond to requests"
  (let [sys (make-system)
        req {:url "http://localhost:8888"}] 
    (:server sys) =not=> nil?
    (start-system sys) => anything
    ; Now that the system is started, we should be able to connect to it 
    (do-request req) => (contains {:status 200})
    (fact "stopping the system stops the webserver"
      (stop-system sys) => anything
      (do-request req) => (throws))))

(defaction ac-add1 
  ; Add 1 to items
  (write :items nil request)
  ; Return the count of items
  (count (query :items nil)))

(future-facts "app state survives restart"
  (let [dispatches {:add ac-add1}
        sysoverrides {:dispatcher (->Dispatcher (atom dispatches))}
        sys (make-system sysoverrides)]
    (start-system sys) => anything
    (do-request [sys :add 1]) => 1
    (do-request [sys :add 1]) => 2
    (stop-system sys) => anything
    (start-system sys) => anything
    (do-request [sys :add 1]) => 3))
