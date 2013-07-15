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

(def json-req {:url "http://localhost:8099/api" :method :json :body {:action :default :a 1}})
(def get-req {:url "http://localhost:8099/api?a=1&action=default"})

(let [serv (->JettyServer 8099 (atom nil) (atom nil))
      sys {:dispatcher (->Dispatcher (atom {:default ac-echo}))
           :renderer (->JsonRenderer)}
      handler (ring-handler (action-handler sys))]
  (set-handler- serv handler) => anything
  (start- serv) => anything
  (facts "JettyServer handles JSON with ring-handler"
    (do-request json-req) => {:a 1 :action "default"})
  (fact "JettyServer denies get api calls by default"
    (do-request get-req) => throws)
  (stop- serv) => anything
  (fact "JettyServer handles get request is explicitly enabled"
    (set-handler- serv 
                  (ring-handler 
                    (action-handler (assoc sys :allow-api-get? true))))
    (start- serv)
    (<-json (do-request get-req)) => (contains {:a "1"})
    (stop- serv)))

(defmodel Cog {:amt Int})
(defn template-edit [model] "<p>Hello</p>")
(defn cog-foo [] "Instance")

(facts "find-in-ns"
  (find-in-ns 'cludje.server-test "Cog") => #'Cog
  (find-in-ns 'cludje.server-test "FSDasdf") => nil)

(def res-request {:url "http://localhost:8099/css/test.css"})

(def template-request {:url "http://localhost:8099/cog/edit.tpl.html"})

(def template-req-no-ext {:url "http://localhost:8099/cog/edit"})

(def template-instance-req {:url "http://localhost:8099/cog/foo.tpl.html"})

(def template-inst-req-no-ext {:url "http://localhost:8099/cog/foo"})

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
    (fact "...and without a .tpl.html extension"
      (:body (do-request template-req-no-ext)) => "<p>Hello</p>")
    (stop- serv) => anything)
  (facts "JettyServer serves template instances"
    (set-handler- 
      serv (ring-handler 
             (template-instance-handler 
               'cludje.server-test 
               'cludje.server-test))) => anything
    (start- serv) => anything
    (:body (do-request template-instance-req)) => "Instance"
    (fact "...and without a .tpl.html extension"
      (:body (do-request template-inst-req-no-ext)) => "Instance")
    (stop- serv)))


(future-facts "JettyServer serves static template files without a .tpl.html extension")

