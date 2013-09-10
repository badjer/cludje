(ns cludje.serve
  (:use cludje.system
        cludje.util
        cludje.web)
  (:require [ring.adapter.jetty :as jetty]
            [cheshire.core :as cheshire]
            [ring.util.response :as response])
  (:import [org.eclipse.jetty.server.handler GzipHandler]))


(defrecord TestServer [default-session session]
  IServer
  (start [self system pipeline]
    "Returns a function that takes input, constructs
     a request from it, assocs it with session, passes
     it to handler, and returns the result.
     Session will be persisted between requests until
     the server is stopped"
    (reset! session @default-session)
    (fn [input]
      (let [res (pipeline {:params input :session @session})]
        (when-let [out-session (:session res)]
          (reset! session out-session))
        (:result res))))
  (stop [self]
    (reset! session nil)))

(defn >TestServer 
  ([] (>TestServer {}))
  ([default-session] (->TestServer (atom default-session) (atom nil))))


(defn- jetty-configurator [server]
  "Ask Jetty to gzip."
  (.setHandler server
               (doto (new GzipHandler)
                 (.setMimeTypes "text/html,text/plain,text/xml,application/xhtml+xml,text/css,application/javascript,text/javascript,image/svg+xml,application/json,application/clojure")
                 (.setHandler (.getHandler server)))))

(defn- jetty-opts [config]
  (merge {:port 8888 :join? false
          :configurator jetty-configurator}
         config))

(defn render-json [response]
  (let [result (?! response :result)]
    (assert-json-renderable result)
    (merge response 
           (-> {:body (cheshire/generate-string result)} 
               (response/content-type "application/json") 
               (response/charset "UTF-8")))))

(defn wrap-render-json [f]
  (fn [request]
    (render-json (f request))))

(defn >web-handler [pipeline]
  (-> pipeline
      (wrap-render-json)
      (wrap-web-exception-handling)
      (wrap-ring-middleware)))

(defrecord JettyServer [jetty-instance]
  IServer
  (start [self system pipeline]
    (let [web-handler (>web-handler pipeline)]
      (reset! jetty-instance 
              (jetty/run-jetty web-handler (jetty-opts system)))))
  (stop [self]
    (when @jetty-instance 
      (.stop @jetty-instance))))

(defn >JettyServer []
  (->JettyServer (atom nil)))

