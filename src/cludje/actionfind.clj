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

(defrecord NSActionFinder [action-namespaces]
  IActionFinder
  (find-action [self context] 
    (let [action-str (? context [:parsed-input :_action])
          finds (keep identity (map #(find-in-ns % action-str) @action-namespaces))
          matches (filter looks-like-action? finds)]
      (if-not (empty? matches)
        (first matches)
        (cond 
          (empty? finds)
          (throw-error {:parsed-action (str "Couldn't find action! Couldn't find anything "
                                     "named " action-str " in the namespaces "
                                     (s/join ", " @action-namespaces))})
          :else 
          (throw-error {:parsed-action (str "Couldn't find action! Found these: "
                                     (s/join ", " finds) ", but none of them "
                                     "looked like actions")}))))))

(defn >NSActionFinder [& action-namespaces]
  (->NSActionFinder (atom action-namespaces)))

