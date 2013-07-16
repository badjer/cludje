(ns cludje.actionparser
  (:require [clojure.string :as s])
  (:use cludje.core))

(defrecord ActionParser []
  IActionParser
  (get-action-name- [self input]
    (get input :_action))
  (get-model-name- [self input]
    (when-let [action (get-action-name- self input)]
      (s/capitalize (first (s/split action #"-")))))
  (get-action-key- [self input]
    (when-let [action (get-action-name- self input)]
      (when-let [s (second (s/split action #"-"))]
        (keyword s)))))

