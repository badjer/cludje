(ns cludje.actionparser
  (:require [clojure.string :as s])
  (:use cludje.core))

(defrecord ActionParser []
  IActionParser
  (get-action-name- [self input]
    (get input :_action))
  (get-model-name- [self input]
    (when-let [action (get-action-name- self input)]
      (when-let [s (second (reverse (s/split action #"-")))]
        (s/capitalize s))))
  (get-action-key- [self input]
    (when-let [action (get-action-name- self input)]
      (when-let [s (last (s/split action #"-"))]
        (keyword s)))))
