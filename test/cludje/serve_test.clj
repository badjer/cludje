(ns cludje.serve-test
  (:use midje.sweet
        cludje.serve
        cludje.system
        cludje.application
        cludje.model
        cludje.types
        cludje.test))

(def req {:url "http://localhost:8099"})

(facts ">JettyServer"
  (let [serv (>JettyServer 8099 identity)]
    (start serv) => anything 
    (do-request req) => (contains {:status 200})
    (stop serv) => anything
    (do-request) => (throws)))

(defn new-cog [{:keys [input]}] {:name "A"})
(def cog (>Model "cog" {:name Str} {}))

(def json-req {:url "http://localhost:8099/api" :method :json 
               :body {:_action "new-cog" :name "B"}})
(def get-req {:method :get-json 
              :url "http://localhost:8099/api?name=B&_action=new-cog"})

(def sys-config {:action-namespaces ['cludje.serve-test]
                 :mold-namespaces ['cludje.serve-test]})


(facts ">JettyServer"
  (let [sys (with-web (>test-system sys-config))
        pipeline (>api-pipeline sys)
        server (>JettyServer 8099 pipeline)]
    (start server) => anything
    (fact "handles GET request"
      (do-request get-req) => {:name "A"})
    ;(fact "handles JSON input"
      ;(do-request json-req) => {:a 1})
    (stop server) => anything))

;(let [pipeline (>api-pipeline
      ;serv (>JettyServer 8099)
      ;sys (make-system {:default-action ac-echo
                        ;:allow-api-get? true
                        ;:login (make-MockLogin {:logged-in? true})
                        ;:model-ns 'cludje.server-test})


      ;handler (ring-handler (action-handler sys))]
  ;(set-handler- serv handler) => anything
  ;(start- serv) => anything
  ;(facts "JettyServer handles JSON with ring-handler"
    ;(do-request json-req) => {:a 1 :_action "default"})
  ;(fact "JettyServer denies get api calls by default"
    ;(do-request get-req) => throws)
  ;(stop- serv) => anything
  ;(fact "JettyServer handles get request is explicitly enabled"
    ;(set-handler- serv 
                  ;(ring-handler 
                    ;(action-handler (assoc sys :allow-api-get? true))))
    ;(start- serv)
    ;(<-json (do-request get-req)) => (contains {:a "1"})
    ;(stop- serv)))
;
;(defmodel Cog {:amt Int})
;(defn -template-edit [model] "<p>Hello</p>")
;(defn cog-foo [] "Instance")
;
;(def res-request {:url "http://localhost:8099/css/test.css"})
;(def template-request {:url "http://localhost:8099/cog/edit.tpl.html"})
;(def template-req-no-ext {:url "http://localhost:8099/Cog/edit"})
;(def template-instance-req {:url "http://localhost:8099/cog/foo.tpl.html"})
;(def template-inst-req-no-ext {:url "http://localhost:8099/Cog/foo"})
;
;(let [serv (->JettyServer 8099 (atom nil) (atom nil))
      ;sys (make-system {:default-action ac-echo 
                        ;;:model-ns 'cludje.server-test
                        ;:template-ns 'cludje.server-test})
      ;templatestore (make-templatestore sys)
      ;fullsys (assoc sys :templatestore templatestore)]
  ;(facts "JettyServer serves static files"
    ;(set-handler- serv (ring-handler (template-handler fullsys))) => anything
    ;(start- serv) => anything
    ;(:body (do-request res-request)) => "hello world\n"
    ;(stop- serv) => anything)
  ;(facts "JettyServer serves templates"
    ;(set-handler- serv (ring-handler (template-handler fullsys))) => anything
    ;;(start- serv) => anything
    ;(:body (do-request template-request)) => "<p>Hello</p>"
    ;(fact "...and without a .tpl.html extension"
      ;(:body (do-request template-req-no-ext)) => "<p>Hello</p>")
    ;(stop- serv) => anything)
  ;(facts "JettyServer serves template instances"
    ;;(set-handler- serv (ring-handler (template-handler fullsys))) => anything
    ;(start- serv) => anything
    ;;(:body (do-request template-instance-req)) => "Instance"
    ;(fact "...and without a .tpl.html extension"
      ;(:body (do-request template-inst-req-no-ext)) => "Instance")
    ;(stop- serv)))

