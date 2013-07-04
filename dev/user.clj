(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.test :as test]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [cludje.app :as app]))

(def system nil)

(def sys-opts {:port 8123 :controller-ns 'user})

(defn init
  "Constructs the current development system."
  []
  (alter-var-root #'system
    (constantly (app/make-system {:port 8123}))))

(defn start
  "Starts the current development system."
  []
  (alter-var-root #'system app/start-system))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system
    (fn [s] (when s (app/stop-system s)))))

(defn go
  "Initializes the current development system and starts it running."
  []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))
