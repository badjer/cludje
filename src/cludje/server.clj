(ns cludje.server
  (:use cludje.core)
  (:require [ring.adapter.jetty :as jetty]))


(import '[org.eclipse.jetty.server.handler GzipHandler])
(defn- jetty-configurator [server]
  "Ask Jetty to gzip."
  (.setHandler server
               (doto (new GzipHandler)
                 (.setMimeTypes "text/html,text/plain,text/xml,application/xhtml+xml,text/css,application/javascript,text/javascript,image/svg+xml,application/json,application/clojure")
                 (.setHandler (.getHandler server)))))

(defn- jetty-opts [config]
  (merge {:port 8080 :join? false
          :configurator jetty-configurator}
         config))

(defrecord JettyServer [port handler server]
  IServer
  (set-handler [self newhandler]
    (reset! handler newhandler))
  IStartable
  (start [self]
    (reset! server (jetty/run-jetty @handler (jetty-opts self))))
  (stop [self]
    (when @server (.stop @server))))
  ;IPersistent
  ;(get-state [self])
  ;(init-state [self state]))

