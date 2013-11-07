(ns cludje.model
  (:require [clojure.string :as s])
  (:use cludje.mold
        cludje.util
        cludje.types))


(defprotocol IModel
  (modelname [self])
  (tablename [self])
  (keyname [self]))

(defn extend-imodel [obj fs opts]
  (let [modelname (s/lower-case (name (?! opts :modelname)))
        tablename (s/lower-case (name (get opts :tablename modelname)))
        no-key? (:no-key opts)
        kee (if no-key? nil :_id)]
    (extend (type obj)
      IModel
      {:modelname (fn [self] modelname)
       :tablename (fn [self] tablename)
       :keyname (fn [self] kee)})
    obj))

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

(defn >Model [fs opts]
  (let [no-key? (:no-key opts)
        kee (if no-key? nil :_id)
        allfields (if no-key?  fs (assoc fs kee Str))
        ; Don't include kee in required fields
        required-fields (get opts :required (vec (keys fs)))
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
        (extend-imodel fs opts)
        (extend-imold allfields mold-opts)
        (extend-ivalidateable)
        (extend-ishowable)
        (extend-iparseable))))
