(ns cludje.altnamespace
  (:use cludje.mold
        cludje.types))

(defn altns-action [context])

(def altnsmold (>Mold {:name Str} {}))
