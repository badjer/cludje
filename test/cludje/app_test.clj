(ns cludje.app-test
  (:use midje.sweet
        cludje.test
        cludje.types
        cludje.core
        cludje.dispatcher
        cludje.renderer
        cludje.login
        cludje.app))

(facts "make-system initializes all subsystems"
  (make-system) => (has-keys :db :mailer :logger :auth :dispatcher
                             :renderer :server))

(facts "make-system allows overriding subsystems"
  (let [dispatcher (->Dispatcher (atom {}))
        sysoverrides {:dispatcher dispatcher}
        sys (make-system sysoverrides)]
    (:dispatcher sys) => dispatcher))


(def req {:url "http://localhost:8888/api" :method :json :body {:_action :default :a 1}})

(fact "a start-system -ed system can respond to requests"
  (let [sys (make-system {:login (make-MockLogin {:logged-in? true})
                          :default-action hello-world})]
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
        login (make-MockLogin {:logged-in? true})
        sysoverrides {:dispatcher dispatcher :renderer renderer :login login}
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
        login (make-MockLogin {:logged-in? true})
        sysoverrides {:dispatcher dispatcher :renderer renderer :login login}
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
; This is a generic template, it provides the edit template for any model
(defn template-edit [model] "Template")
; This is an instance template - it provides the foo template for cog
(defn cog-foo [] "Instance")

(def template-request {:url "http://localhost:8888/cog/edit.tpl.html"})
(def bad-template-request {:url "http://localhost:8888/cog/foosuamsdf.tpl.html"})
(def template-instance-req {:url "http://localhost:8888/cog/foo.tpl.html"})

(fact "making a system with :template-ns and :model-ns set 
  has it serve templates"
  (let [sys (make-system {:template-ns 'cludje.app-test
                          :model-ns 'cludje.app-test})]
    (start-system sys) => anything
    (do-request template-request) => (contains {:status 200})
    (do-request bad-template-request) => (throws)
    (fact "Can also do template instances"
      (:body (do-request template-instance-req)) => "Instance"
    (stop-system sys))))

