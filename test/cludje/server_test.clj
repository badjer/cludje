(ns cludje.server-test
  (:use midje.sweet
        cludje.testcontrollers
        cludje.test
        cludje.core
        cludje.app
        cludje.types
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

(def json-req {:url "http://localhost:8099" :method :json :body {:action :default :a 1}})

(facts "action-handler returns default action"
  (let [sys {:dispatcher (->Dispatcher (atom {:default ac-echo}))
             :renderer (->LiteralRenderer)}
        handler (action-handler sys)]
    (handler json-req) =not=> nil?
    (handler json-req) => {:action :default :a 1}))

(facts "JettyServer handles JSON with ring-handler"
  (let [serv (->JettyServer 8099 (atom nil) (atom nil))
        sys {:dispatcher (->Dispatcher (atom {:default ac-echo}))
             :renderer (->JsonRenderer)}
        handler (ring-handler (action-handler sys))]
    (set-handler- serv handler) => anything
    (start- serv) => anything
    (do-request json-req) => {:a 1 :action "default"}
    (stop- serv) => anything))

(defmodel Cog {:amt Int})
(defn edit-template [model] "<p>Hello</p>") 

(facts "find-in-ns"
  (find-in-ns 'cludje.server-test "Cog") => #'Cog
  (find-in-ns 'cludje.server-test "FSDasdf") => nil)

(def res-request {:url "http://localhost:8099/css/test.css"})

(def template-request {:url "http://localhost:8099/templates/Cog/edit.tmpl.html"})

(let [serv (->JettyServer 8099 (atom nil) (atom nil))
      sys {:dispatcher (->Dispatcher (atom {:default ac-echo}))
           :renderer (->JsonRenderer)}]
  (facts "JettyServer serves static files"
    (set-handler- serv (ring-handler (action-handler sys))) => anything
    (start- serv) => anything
    (:body (do-request res-request)) => "hello world\n"
    (stop- serv) => anything)
  (facts "JettyServer serves templates"
    (set-handler- serv (ring-handler 
                         (template-handler 'cludje.server-test 
                                           'cludje.server-test))) => anything
    (start- serv) => anything
    (:body (do-request template-request)) => "<p>Hello</p>"
    (stop- serv) => anything))




