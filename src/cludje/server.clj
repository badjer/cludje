(ns cludje.server
  (:use cludje.core)
  (:require [cludje.templates.angular :as ng]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.json :as json]
            [ring.middleware.resource :as resource]
            [ring.middleware.file-info :as file-info]
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

(defrecord JettyServer [port handler server]
  IServer
  (set-handler- [self newhandler]
    (reset! handler newhandler))
  IStartable
  (start- [self]
    (reset! server (jetty/run-jetty @handler (jetty-opts self))))
  (stop- [self]
    (when @server (.stop @server))))
  ;IPersistent
  ;(get-state [self])
  ;(init-state [self state]))


(defn jetty [port]
  (->JettyServer port (atom nil) (atom nil)))

(defn make-ring-handler-l [sys]
  (fn [request]
    (ng/angular-layout "Test" {}
                       (ng/list-model LoginUser {:username "abc"}))))

(def template-regex #"/templates/([^/]+)/([^/.]+)\.tmpl\.html$")

(defn get-modelname [request]
  (when-let [uri (:uri request)]
    (second (re-find template-regex uri))))

(defn get-templatename [request]
  (when-let [uri (:uri request)]
    (when-let [n (last (re-find template-regex uri))]
      (str n "-template"))))


(defn find-in-ns [nas thing]
  (when (and nas thing)
    (if (symbol? thing)
      (ns-resolve nas thing)
      (ns-resolve nas (symbol thing)))))

(defn html-response [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str body)})

(defn template-handler [model-ns template-ns]
  "Generates a fn that returns template for the model, provided that model
  exists and that template-model exists"
  (fn [request]
    (let [modelname (get-modelname request)
          templatename (get-templatename request)
          model (find-in-ns model-ns modelname)
          template (find-in-ns template-ns templatename)]
      (when (and template model)
        (html-response (template model))))))

(defn request-data [request]
  (get request :params (get request :body)))

(defn action-handler [{:keys [dispatcher renderer] :as sys}]
  "Generates a fn that runs an action"
  (fn [request]
    (let [data (request-data request)]
    (when-let [action (get-action dispatcher data)]
      (render renderer request (action sys data))))))

(defn ring-handler [& handlers]
  (let [handlers (filter identity handlers)]
    (-> (fn [request]
          (first (map #(% request) handlers))) 
        (resource/wrap-resource "public")
        (file-info/wrap-file-info) 
        (kw/wrap-keyword-params) 
        (json/wrap-json-params))))



(defrecord MockServer []
  IServer
  (set-handler- [self newhandler]))
  
