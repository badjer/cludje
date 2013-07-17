(ns cludje.server
  (:use cludje.core
        cludje.templatestore)
  (:require [clojure.string :as s]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.json :as json]
            [ring.middleware.resource :as resource]
            [ring.middleware.file-info :as file-info]
            [ring.middleware.params :as params]
            [ring.middleware.keyword-params :as kw]))


(import '[org.eclipse.jetty.server.handler GzipHandler])
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

(defrecord JettyServer [port handler jetty-instance]
  IServer
  (set-handler- [self newhandler]
    (reset! handler newhandler))
  IStartable
  (start- [self]
    (reset! jetty-instance (jetty/run-jetty @handler (jetty-opts self))))
  (stop- [self]
    (when @jetty-instance (.stop @jetty-instance))))
;IPersistent
;(get-state [self])
;(init-state [self state]))


(defn jetty [{:keys [port]}]
  (->JettyServer port (atom nil) (atom nil)))

; TODO: Cleanup this mess - we need some abstraction around 
; some of this stuff - the server is doing too much
(def template-regex #"/([^/]+)/([^/.]+)")

(defn server-get-modelname [request]
  (when-let [uri (:uri request)]
    (when-let [n (second (re-find template-regex uri))]
      (s/capitalize n))))

(defn server-get-action-key [request]
  (when-let [uri (:uri request)]
    (when-let [n (last (re-find template-regex uri))]
      n)))

(defn html-response [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str body)})


(defn template-handler
  "Generates a fn that serves templates"
  ([{:keys [templatestore] :as system}]
   (fn [request]
     (let [modelname (server-get-modelname request)
           action-key (server-get-action-key request)
           res (get-template- templatestore modelname action-key)]
       (when res
         (html-response res))))))


(defn action-handler 
  "Generates a fn that runs an action"
  ([{:keys [renderer parser] :as system}]
   (fn [request]
     (when-let [input (parse-input- parser request)]
       (render- renderer request (do-action system input))))))

(defn ring-handler [& handlers]
  (let [handlers (filter identity handlers)]
    (-> (fn [request]
          (first (filter identity (map #(% request) handlers))))
        (resource/wrap-resource "public")
        (file-info/wrap-file-info) 
        (kw/wrap-keyword-params) 
        (json/wrap-json-params)
        (params/wrap-params))))


(defrecord MockServer []
  IServer
  (set-handler- [self newhandler]))

