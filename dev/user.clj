(ns user
  (:use cludje.datastore 
        cludje.application
        midje.repl)
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.test :as test]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]))

(def system nil)

(defn get-db []
  (when-let [ds (slurp-testdatastore "db.txt")]
    {:data-store ds}))

(defn put-db []
  (when-let [ds (:data-store system)]
    (spit-testdatastore ds "db.txt")))

(def config {:action-namespaces ['cludje.demo.actions]
             :mold-namespaces ['cludje.demo.models]})


(defn >system []
  (merge
    (with-web (>test-system config))
    (get-db)
    {:port 8088}))

(defn init
  "Constructs the current development system."
  []
  (alter-var-root #'system (fn [s] (>system))))

(defn start
  "Starts the current development system."
  []
  (alter-var-root #'system 
                  (fn [s]
                    (if-not [s]
                      (println "System wasn't init'd.")
                      (start-system s)))))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system
    (fn [s] (when s (stop-system s)))))

(defn go
  "Initializes the current development system and starts it running."
  []
  (init)
  (start))

(defn reset []
  (put-db)
  (stop)
  (refresh :after 'user/go))
