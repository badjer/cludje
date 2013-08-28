(ns cludje.model
  (:require [clojure.string :as s])
  (:use cludje.mold
        cludje.types))


(defprotocol IModel
  (mold [self])
  (tablename [self])
  (keyname [self])
  (partitions [self]))

(defn extend-imodel [obj tablename fs opts]
  (let [tablename (s/lower-case (name tablename))
        no-key? (:no-key opts)
        kee (if no-key? nil :_id)
        parts (get opts :partitions [])]
    (extend (type obj)
      IModel
      {:tablename (fn [self] tablename)
       :keyname (fn [self] kee)
       :partitions (fn [self] parts)})
    obj))

; We want strings and keywords to act as models,
; for when people pass strings instead of models
; to db functions, for example
(extend String
  IModel
  {:tablename (fn [self] (s/lower-case self))
   :keyname (fn [self] :_id)
   :partitions (fn [self] [])})
(extend clojure.lang.Keyword
  IModel
  {:tablename (fn [self] (s/lower-case (name self)))
   :keyname (fn [self] :_id)
   :partitions (fn [self] [])})

(defn >Model [tablename fs opts]
  (let [no-key? (:no-key opts)
        kee (if no-key? nil :_id)
        allfields (if no-key?  fs (assoc fs kee Str))
        ; Don't include kee in required fields
        required-fields (vec (keys fs))
        invisible-fields (conj (get opts :invisible []) kee)
        mold-opts (merge opts {:required required-fields 
                               :invisible invisible-fields})]
    ; Terrible hack here.
    ; We want to create a new type at runtime. We can't just
    ; call (reify), it seems, because it gives back the same
    ; type every time.
    ; So instead, we eval in order to get new types
    ; NOTE: If specify gets added to Clojure, that's what we'd like here
    (-> (eval '(reify))
        (extend-imodel tablename fs opts)
        (extend-imold allfields mold-opts)
        (extend-ivalidateable)
        (extend-ishowable)
        (extend-iparseable))))
