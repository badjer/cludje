(ns user
  (:use cludje.core
        cludje.login
        cludje.database
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
    (constantly (let [sys (make-system {:port 8123 
                                        :db (->MemDb (atom {:user [mockuser]}))
                                        :default-action nil 
                                        :template-ns 'cludje.demo.templates 
                                        :model-ns 'cludje.demo.models 
                                        :action-ns 'cludje.demo.actions})]
                  (assoc sys :login (->TokenLogin "abc" (:db sys) :user))))))
                              ;:login (make-TestLogin nil)


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
