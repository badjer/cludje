(ns cludje.actionstore
  (:use cludje.core))

(defrecord ActionStore [action-ns default-action]
  IActionStore
  (get-action- [self action-name]
    (if-let [act (find-in-ns action-ns action-name :cludje-action)]
      act
      default-action)))
