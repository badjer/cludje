(ns cludje.model
  (:require [clojure.string :as s])
  (:use cludje.mold
        cludje.types))


(defprotocol IModel
  (mold [self])
  (tablename [self])
  (keyname [self])
  (partitions [self]))


(defn ->Model [tablename fs opts]
  (let [tablename (s/lower-case (name tablename))
        no-key? (:no-key opts)
        kee (if no-key? nil :_id)
        allfields (if no-key?  fs (assoc fs kee Str))
        ; Don't include kee in required fields
        required-fields (vec (keys fs))
        parts (get opts :partitions [])
        invisible-fields (conj (get opts :invisible []) kee)
        mold-opts (merge {:required required-fields
                          :invisible invisible-fields})
        model-mold (->Mold allfields mold-opts)]
    (-> (reify IModel
          (mold [self] model-mold)
          (tablename [self] tablename)
          (keyname [self] kee)
          (partitions [self] parts)))))

