(ns cludje.dispatcher
  (:use cludje.core)
  (:require [clojure.string :as s]))

(defrecord Dispatcher [dispatches]
  IDispatcher
  (get-action- [self request]
    (let [default (get @dispatches :default)]
      (if-let [action (get request :action)]
        (get @dispatches (keyword (name action)) default)
        default))))


(defn is-action? [vr]
  (let [m (meta vr)]
    (get m :action)))

(defn find-actions [root-ns]
  ; Find all the defactions under the specified namespace
  ; (including all namespaces that start with it)
  ; and build them into a dictionary that we can 
  ; give to a Dispatcher
  (when root-ns
    (let [ns-str (s/replace (str "/" (name root-ns)) "." "/")]
      (load ns-str)
      (into {} (for [[k v] (ns-publics root-ns)]
                 (when (is-action? v)
                   [(keyword (name k)) v])))
    )))

(defn make-dispatcher 
  ([root-ns]
   (make-dispatcher root-ns {}))
  ([root-ns otherdispatches]
    (let [dispatches (find-actions root-ns)
          disp (merge dispatches otherdispatches)]
      (->Dispatcher (atom disp)))))

