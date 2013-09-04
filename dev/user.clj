(ns user
  (:use cludje.datastore 
        midje.repl)
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.test :as test]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]))

(def system nil)

(def mockuser {:email "a@b.cd" :password "123"})

(defn get-db []
  (or (slurp-testdatastore "db.txt") 
      (>TestDatastore {:user [mockuser]})))

(defn put-db []
  (when-let [db (:data-store system)]
    (spit-testdatastore db "db.txt")))

;(defn init
  ;"Constructs the current development system."
  ;[]
  ;(alter-var-root #'system
    ;(constantly (let [sys (make-system {:port 8123 
                                        ;:db (get-db)
                                        ;:default-action nil 
                                        ;:template-ns 'cludje.demo.templates 
                                        ;:model-ns 'cludje.demo.models 
                                        ;:action-ns 'cludje.demo.actions})]
                  ;(assoc sys :login (->TokenLogin "abc" (:db sys) :user))))))
                              ;;:login (make-TestLogin nil)
;

;(defn start
  ;"Starts the current development system."
  ;[]
  ;(alter-var-root #'system start-system))

;(defn stop
  ;"Shuts down and destroys the current development system."
  ;[]
  ;(alter-var-root #'system
    ;(fn [s] (when s (stop-system s)))))

;(defn go
  ;"Initializes the current development system and starts it running."
  ;[]
  ;(init)
  ;(start))

;(defn reset []
  ;(put-db)
  ;(stop)
  ;(refresh :after 'user/go))
