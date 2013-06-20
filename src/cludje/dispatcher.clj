(ns cludje.dispatcher
  (:use cludje.core)
  (:require [clojure.string :as s]))

(defrecord Dispatcher [dispatches]
  IDispatcher
  (get-action- [self request]
    (when-let [action (get request :action)]
      (get @dispatches (keyword (name action))))))


(defn find-actions [root-ns]
  ; Find all the defactions under the specified namespace
  ; (including all namespaces that start with it)
  ; and build them into a dictionary that we can 
  ; give to a Dispatcher
  (let [ns-str (s/replace (name root-ns) #"^[^\.]+\." "")]
    (load ns-str)
    (into {} (for [[k v] (ns-publics root-ns)]
               [(keyword (name k)) v]))
  ))

(defn make-dispatcher [root-ns]
  (let [dispatches (find-actions root-ns)]
    (->Dispatcher (atom dispatches))))
