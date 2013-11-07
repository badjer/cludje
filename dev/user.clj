(ns user
  (:use cludje.datastore 
        cludje.test
        cludje.system
        cludje.serve
        midje.repl)
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.test :as test]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]))

(def system (atom nil))
(def server (atom nil))

(defn get-db []
  (when-let [ds (slurp-testdatastore "db.txt")]
    {:data-store ds}))

(defn put-db []
  (when-let [ds (:data-store system)]
    (spit-testdatastore ds "db.txt")))

(def config {})

(defn >system []
  (merge
    (>test-system config)
    (get-db)
    {:port 8086}))

(defn init []
  "Constructs the current development system."
  (reset! server (>JettyServer))
  (reset! system (>system)))

(defn begin []
  "Starts the current development system."
  (start @server @system))

(defn end []
  "Shuts down and destroys the current development system."
  (stop @server))

(defn go
  "Initializes the current development system and starts it running."
  []
  (init)
  (begin))

(defn reset []
  (put-db)
  (end)
  (refresh :after 'user/go))
