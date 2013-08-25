(ns cludje.moldstore
  (:use cludje.system))

(defrecord SingleMoldStore [mold]
  IMoldStore
  (get-mold [self context] @mold))

(defn >SingleMoldStore [mold]
  (->SingleMoldStore (atom mold)))
