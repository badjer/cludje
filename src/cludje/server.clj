(ns cludje.server
  (:use cludje.core)
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

(def template-regex #"/([^/]+)/([^/.]+)")

(defn get-modelname [request]
  (when-let [uri (:uri request)]
    (when-let [n (second (re-find template-regex uri))]
      (s/capitalize n))))

(defn get-templatename [request]
  (when-let [uri (:uri request)]
    (when-let [n (last (re-find template-regex uri))]
      (str "template-" n))))

(defn get-template-instance-name [request]
  (when-let [uri (:uri request)]
    (when-let [n (last (re-find template-regex uri))]
      (str (s/lower-case (get-modelname request)) "-" n))))


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
        (html-response (template @model))))))

(defn template-instance-handler [model-ns template-ns]
  "Generates a fn that returns specific templates for a model.
  ie, if the app defined a get-project template, then calls to
  /templates/Project/get.tpl.html would get routed to it"
  (fn [request]
    (let [templatename (get-template-instance-name request)
          template (find-in-ns template-ns templatename)]
      (when template
        (html-response (template))))))

(defn request-data [request]
  (get request :params (get request :body)))

(defn action-handler 
  "Generates a fn that runs an action"
  ([{:keys [dispatcher renderer] :as sys}]
    (let [allow-get? (get sys :allow-api-get? false)
          post-check (if allow-get? 
                       identity 
                       #(= (:request-method %) :post))
          api-uri-check #(= "/api" (:uri %))
          is-api-call? #(and (api-uri-check %) (post-check %))]
      (fn [request]
        (let [data (request-data request)]
          (when (is-api-call? request)
            (when-let [action (get-action dispatcher data)]
              (render renderer request (action sys data)))))))))

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

