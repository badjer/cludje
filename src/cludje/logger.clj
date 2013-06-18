(ns cludje.logger
  (:use cludje.core))

(defrecord MemLogger [logatom]
  ILogger
  (log- [self message]
    (swap! logatom conj message)))

(defrecord ConsoleLogger []
  ILogger
  (log- [self message]
    (println message)))
