(ns cludje.logger
  (:use cludje.core))

(defrecord ConsoleLogger []
  ILogger
  (log- [self message]
    (println message)))
