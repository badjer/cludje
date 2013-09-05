(ns cludje.serve
  (:use cludje.system)
  (:require [ring.adapter.jetty :as jetty])
  (:import [org.eclipse.jetty.server.handler GzipHandler]))


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
  (start [self]
    (reset! jetty-instance (jetty/run-jetty @handler (jetty-opts self))))
  (stop [self]
    (when @jetty-instance (.stop @jetty-instance))))

(defn >JettyServer [port handler]
  (->JettyServer port (atom handler) (atom nil)))
