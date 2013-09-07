(ns cludje.serve
  (:use cludje.system
        cludje.web)
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

(defrecord JettyServer [port jetty-instance]
  IServer
  (start [self start-port handler]
    (let [safe-handler (wrap-web-exception-handling handler)]
      (reset! port start-port)
      (reset! jetty-instance 
              (jetty/run-jetty safe-handler (jetty-opts {:port start-port})))))
  (stop [self]
    (when @jetty-instance 
      (.stop @jetty-instance)
      (reset! port nil))))

(defn >JettyServer []
  (->JettyServer (atom nil) (atom nil)))

