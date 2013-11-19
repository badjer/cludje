(ns cludje.actionfind
  (:use cludje.system
        cludje.util
        cludje.errors
        cludje.find)
  (:require [clojure.string :as s]))

(defrecord SingleActionFinder [action]
  IActionFinder
  (find-action [self request] action))

(defn >SingleActionFinder [action]
  (->SingleActionFinder action))


(defn sym-to-action [sym]
  (when sym
    (when (resolve sym)
      (when-let [a @(resolve sym)]
        (when (and (fn? a) (= 1 (arity a)))
          a)))))

(defn- find-action- [request action-namespaces throw?]
  (let [action-str (?! request [:params :_action])
        finds (find-in-nses action-namespaces action-str)
        matches (keep sym-to-action finds)]
    (if (seq matches)
      (first matches)
      (when throw?
        (cond 
          (empty? finds)
          (throw-not-found {:params (str "Couldn't find action! Couldn't find anything "
                                     "named " action-str " in the namespaces "
                                     (s/join ", " action-namespaces))})
          :else 
          (throw-not-found {:params (str "Couldn't find action! Found these: "
                                     (s/join ", " finds) ", but none of them "
                                     "looked like actions")}))))))


(defrecord NSActionFinder [action-namespaces]
  IActionFinder
  (find-action [self request] 
    (find-action- request @action-namespaces true)))

(defn >NSActionFinder [& action-namespaces]
  (->NSActionFinder (atom action-namespaces)))
