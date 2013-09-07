(ns cludje.actionfind
  (:use cludje.system
        cludje.util
        cludje.errors
        cludje.find)
  (:require [clojure.string :as s]))

(defrecord SingleActionFinder [action-sym]
  IActionFinder
  (find-action [self context] action-sym))

(defn >SingleActionFinder [action-sym]
  (->SingleActionFinder action-sym))


(defn looks-like-action? [sym]
  (let [f @(resolve sym)]
    (and (fn? f) (= 1 (arity f)))))

(defn- find-action- [context action-namespaces throw?]
  (let [action-str (?! context [:parsed-input :_action])
        finds (find-in-nses action-namespaces action-str)
        matches (filter looks-like-action? finds)]
    (if-not (empty? matches)
      (first matches)
      (when throw?
        (cond 
          (empty? finds)
          (throw-error {:parsed-action (str "Couldn't find action! Couldn't find anything "
                                     "named " action-str " in the namespaces "
                                     (s/join ", " action-namespaces))})
          :else 
          (throw-error {:parsed-action (str "Couldn't find action! Found these: "
                                     (s/join ", " finds) ", but none of them "
                                     "looked like actions")}))))))


(defrecord NSActionFinder [action-namespaces]
  IActionFinder
  (find-action [self context] 
    (find-action- context @action-namespaces true)))


(defn >NSActionFinder [& action-namespaces]
  (->NSActionFinder (atom action-namespaces)))

