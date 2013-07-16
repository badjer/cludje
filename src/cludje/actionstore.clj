(ns cludje.actionstore
  (:use cludje.core))

(defrecord ActionStore [action-ns default-action]
  IActionStore
  (get-action- [self action-name]
    (if-let [vr (find-in-ns action-ns action-name)]
      @vr
      default-action)))
