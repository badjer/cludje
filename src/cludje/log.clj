(ns cludje.log
  (:use cludje.system))

(defrecord TestLogger [entries]
  ILog
  (log [self message]
    (swap! entries conj message)))

(defn >TestLogger []
  (->TestLogger (atom [])))


(defrecord ConsoleLogger []
  ILog
  (log [self message]
    (println message)))

(defn >ConsoleLogger []
  (->ConsoleLogger))
