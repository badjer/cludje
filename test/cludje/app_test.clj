(ns cludje.app-test
  (:use midje.sweet
        cludje.test
        cludje.types
        cludje.core
        cludje.dispatcher
        cludje.renderer
        cludje.app))

(facts "make-system initializes all subsystems"
  (make-system) => (has-keys :db :mailer :logger :auth :dispatcher
                             :renderer :server))

(facts "make-system allows overriding subsystems"
  (let [dispatcher (->Dispatcher (atom {}))
        sysoverrides {:dispatcher dispatcher}
        sys (make-system sysoverrides)]
    (:dispatcher sys) => dispatcher))

(def req {:url "http://localhost:8888" :method :json :body {:action :default :a 1}})

(fact "a start-system -ed system can respond to requests"
  (let [sys (make-system)]
    (start-system sys) => anything
    ; Now that the system is started, we should be able to connect to it 
    (do-request req) => {:msg "hello world"}
    (fact "stopping the system stops the webserver"
      (stop-system sys) => anything
      (do-request req) => (throws))))

(defaction ac-a1 {:a 1})

(fact "started app responds with json"
  (let [dispatches {:default ac-a1}
        dispatcher (->Dispatcher (atom {:default ac-a1}))
        renderer (->JsonRenderer)
        sysoverrides {:dispatcher dispatcher :renderer renderer}
        sys (make-system sysoverrides)]
    (start-system sys) => anything
    (do-request req) => {:a 1}
    (stop-system sys) => anything))

(defaction ac-add1 
  ; Add 1 to items
  (write :items nil {:a 1})
  ; Return the count of items
  {:body (str (count (query :items nil)))})

(facts "app db survives restart"
  (let [dispatches {:default ac-add1}
        dispatcher (->Dispatcher (atom {:default ac-add1}))
        renderer (->LiteralRenderer)
        sysoverrides {:dispatcher dispatcher :renderer renderer}
        sys (make-system sysoverrides)]
    (:dispatcher sys) => dispatcher
    (start-system sys) => anything
    (do-request req) => 1
    (do-request req) => 2
    (stop-system sys) => anything
    (do-request req) => (throws)
    (start-system sys) => anything
    (do-request req) => 3
    (stop-system sys) => anything))

(defmodel Cog {:amt Int})
(def template-request {:url "http://localhost:8888/templates/Cog/edit.tpl.html"})
(def bad-template-request {:url "http://localhost:8888/templates/Cog/foosuamsdf.tpl.html"})

(fact "making a system with :template-ns and :model-ns set 
  has it serve templates"
  (let [sys (make-system {:template-ns 'cludje.templates.angular
                          :model-ns 'cludje.app-test})]
    (start-system sys) => anything
    (do-request template-request) => (contains {:status 200})
    (do-request bad-template-request) => (throws)
    (stop-system sys)))

