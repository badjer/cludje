(ns cludje.integration-test
  (:use midje.sweet
        cludje.test
        cludje.action
        cludje.util
        cludje.serve
        cludje.mold
        cludje.types
        cludje.system
        cludje.application
        cludje.session)
  (:require [clj-http.cookies :as cookies]))


(def Cog (>Mold {:foo-ran Bool :sess Anything} {})) 


(defn foo-cog [context] 
  (with-action-dsl context 
    (output {})))

(defn wasfoo-cog [context] 
  (with-action-dsl context
    (output {:foo-ran (?? context [:session :foo-ran])})))

(defn >json-req 
  ([action] (>json-req action nil))
  ([action input] (>json-req action input (>cookies)))
  ([action input cookies] 
   {:url "http://localhost:8099/api" :method :json :cookies cookies
    :body (merge input {:_action (name action)})}))


(defn foo-req [cookies] (>json-req :foo-cog {} cookies))
(defn wasfoo-req [cookies] (>json-req :wasfoo-cog {} cookies))

(def sys-config {:action-namespaces ['cludje.integration-test]
                 :mold-namespaces ['cludje.integration-test]})

(facts "Store things in session"
  (let [sys (with-web (>test-system sys-config))
        cs (>cookies)
        handler (>api-pipeline sys)
        server (>JettyServer)]
    (start server 8099 handler) => anything
    (do-request (wasfoo-req cs)) => {};(body {:foo-ran nil})

    ;(do-request (foo-req cs)) => (status 200)
    ;(do-request (wasfoo-req cs)) => (body {:foo-ran "yes"})
    (stop server) => anything))
