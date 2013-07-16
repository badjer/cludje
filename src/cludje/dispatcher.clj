(ns cludje.dispatcher
  (:use cludje.core)
  (:require [clojure.string :as s]))

(defrecord Dispatcher [dispatches]
  IDispatcher
  (get-modelname- [self input]
    (when-let [action (get input :_action)]
      (s/capitalize (first (s/split action #"-")))))
  (get-actionkey- [self input]
    (when-let [action (get input :_action)]
      (when-let [s (second (s/split action #"-"))]
        (keyword s))))
  (get-action- [self input]
    (when-let [action (get input :_action)]
      (get @dispatches (keyword (name action))))))

(defn is-action? [vr]
  (let [m (meta vr)]
    (get m :_action)))

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

(defn make-dispatcher [{:keys [action-ns dispatches default-action]}]
  (let [dispatches (find-actions action-ns)
        default-dispatch (when default-action {:default default-action})
        disp (merge dispatches dispatches default-dispatch)]
    (->Dispatcher (atom disp))))

