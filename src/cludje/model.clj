(ns cludje.model
  (:require [clojure.string :as s])
  (:use cludje.mold
        cludje.util
        cludje.types))


(defprotocol IModel
  (modelname [self])
  (tablename [self])
  (keyname [self]))

(defn extend-imodel [clss fs opts]
  (let [modelname (s/lower-case (name (?! opts :modelname)))
        tablename (s/lower-case (name (get opts :tablename modelname)))
        no-key? (:no-key opts)
        kee (if no-key? nil :_id)]
    (extend clss
      IModel
      {:modelname (fn [self] modelname)
       :tablename (fn [self] tablename)
       :keyname (fn [self] kee)})
    clss))

; We want strings and keywords to act as models,
; for when people pass strings instead of models
; to db functions, for example
(extend String
  IModel
  {:modelname (fn [self] (s/lower-case self))
   :tablename (fn [self] (s/lower-case self))
   :keyname (fn [self] :_id)})
(extend clojure.lang.Keyword
  IModel
  {:modelname (fn [self] (s/lower-case (name self)))
   :tablename (fn [self] (s/lower-case (name self)))
   :keyname (fn [self] :_id)})


(defn get-mold-opts [fs opts]
  (let [no-key? (:no-key opts)
        kee (if no-key? nil :_id)
        ; Don't include kee in required fields
        required-fields (get opts :required (vec (keys fs)))
        invisible-fields (conj (get opts :invisible []) kee)] 
    (merge opts {:required required-fields :invisible invisible-fields})))

(defn get-fields [fs opts]
  (let [no-key? (:no-key opts)
        kee (if no-key? nil :_id)] 
    (if no-key? fs (assoc fs kee Str))))

(defmacro defmodel [nam fs & args] 
  "Args are specified in key-value combinations, ie
    (defmodel Car {:name Str} :modelname \"Cars\" :tablename \"Car_tbl\")
  Valid args are
    :required <seq>
    :invisible <seq>
    :defaults <map>
    :names <map>
    :no-key <bool>
    :keyname <str>
    :modelname <str>
    :tablename <str>"
  (let [opts (apply hash-map args)
        opts (if (:modelname opts) 
               opts 
               (assoc opts :modelname (s/lower-case (name nam))))
        classname (symbol (str nam "-type"))
        constructor (symbol (str "->" nam "-type"))
        instance (symbol nam)] 
    `(do 
       (deftype ~classname [])
       (extend-imodel ~classname ~fs ~opts)
       (extend-imold ~classname (get-fields ~fs ~opts) (get-mold-opts ~fs ~opts))
       (extend-iuitype ~classname)
       (def ~instance (~constructor)))))


(defn >Model [fs opts]
  "Note: most of the time you'll want to use defmold instead.
  It will give you better error messages.
  Only use this if you NEED to create an anonymous mold"
  (let [allfields (get-fields fs opts)
        mold-opts (get-mold-opts fs opts)
        ; Terrible hack here.
        ; We want to create a new type at runtime. We can't just
        ; call (reify), it seems, because it gives back the same
        ; type every time.
        ; So instead, we eval in order to get new types
        ; NOTE: If specify gets added to Clojure, that's what we'd like here
        obj (eval '(reify))]
    (-> (type obj)
        (extend-imodel fs opts)
        (extend-imold allfields mold-opts)
        (extend-iuitype))
    obj))
