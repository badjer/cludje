(ns user
  (:use cludje.core
        cludje.app)
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.test :as test]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]))

(def system nil)

(defn init
  "Constructs the current development system."
  []
  (alter-var-root #'system
    (constantly (make-system {:port 8123 
                              :template-ns 'cludje.templates.angular
                              :model-ns 'cludje.demo.models
                              :action-ns 'cludje.demo.actions}))))


(defn start
  "Starts the current development system."
  []
  (alter-var-root #'system start-system))

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
  (stop)
  (refresh :after 'user/go))
