(ns cludje.altnamespace
  (:use cludje.mold
        cludje.types))

(defn altns-action [context])

(def altnsmold (>Mold {:name Str} {}))

(defn template-for-add [model] "Altns add")

(defn inc-cog [] "Altns inc cog")

(defn edit-cog [] "Altns edit cog")
