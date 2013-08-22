(ns cludje.actionfind
  (:use cludje.system))

(defrecord SingleActionFinder [action]
  IActionFinder
  (find-action [self context] action))

(defn >SingleActionFinder [action]
  (->SingleActionFinder action))


