(ns cludje.serve
  (:use cludje.system
        cludje.util
        cludje.pipeline
        cludje.web)
  (:require [clojure.string :as st]
            [ring.adapter.jetty :as jetty]
            [cheshire.core :as cheshire]
            [ring.util.response :as response])
  (:import [org.eclipse.jetty.server.handler GzipHandler]))

(def default-header-buffer-size 65536)
(defn- make-jetty-configurator [config]
  (let [header-buffer-size (get config :header-buffer-size default-header-buffer-size)]
    (fn [server]
      "Ask Jetty to gzip."
      (doseq [connector (.getConnectors server)]
        (.setRequestHeaderSize connector header-buffer-size))
      (.setHandler server
                   (doto (new GzipHandler)
                     (.setMimeTypes "text/html,text/plain,text/xml,application/xhtml+xml,text/css,application/javascript,text/javascript,image/svg+xml,application/json,application/clojure")
                     (.setHandler (.getHandler server))))
      )))


(defn- jetty-opts [config]
  (merge {:port 8888 :join? false
          :configurator (make-jetty-configurator config)}
         config))

(defn jsonify [k]
  (-> k (name) (st/replace "-" "_") (keyword)))

(defn jsonify-keys [m]
  (cond 
    (map? m) (-> m (map-vals jsonify-keys) (map-keys jsonify))
    (sequential? m) (map jsonify-keys m)
    :else m))


(defn render-json [response]
  (let [result (?! response :result)
        prepped-res (jsonify-keys result)]
    (assert-json-renderable prepped-res)
    (merge response 
           (-> {:body (cheshire/generate-string prepped-res)} 
               (response/content-type "application/json") 
               (response/charset "UTF-8")))))

(defn wrap-render-json [f]
  (fn [request]
    (render-json (f request))))

(defn >web-handler [pipeline opts]
  (-> pipeline
      (wrap-render-json)
      (wrap-web-exception-handling)
      (wrap-ring-middleware opts)))

(defrecord JettyServer [pipeline jetty-instance]
  IServer
  (start [self opts]
    (let [web-handler (>web-handler pipeline opts)]
      (reset! jetty-instance 
              (jetty/run-jetty web-handler (jetty-opts opts)))))
  (stop [self]
    (when @jetty-instance 
      (.stop @jetty-instance))))

(defn >JettyServer 
  ([pipeline] (->JettyServer pipeline (atom nil))))

