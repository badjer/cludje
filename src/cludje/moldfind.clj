(ns cludje.moldfind
  (:use cludje.system
        cludje.mold
        cludje.util
        cludje.errors
        cludje.find)
  (:require [clojure.string :as s]))

(defrecord SingleMoldFinder [mold-sym]
  IMoldFinder
  (find-mold [self context] mold-sym))

(defn >SingleMoldFinder [mold-sym]
  (->SingleMoldFinder mold-sym))

(defn is-mold? [sym]
  (let [m @(resolve sym)]
    (satisfies? IMold m)))

(defn propose-moldname [action-sym]
  (let [action (str (name action-sym))
        ; We assume that the action is of the form:
        ; some-ns/operation-model
        unqualified-act (last (s/split action #"/"))
        modelname (last (s/split unqualified-act #"-"))]
    modelname))


(defrecord NSMoldFinder [mold-namespaces]
  IMoldFinder
  (find-mold [self context]
    (let [action-sym (? context :action-sym)
          moldname (propose-moldname action-sym)
          finds (keep identity (map #(find-in-ns % moldname) @mold-namespaces))
          matches (filter is-mold? finds)]
      (if-not (empty? matches)
        (first matches)
        (cond 
          (empty? finds)
          (throw-error {:mold (str "Couldn't find mold! Couldn't find anything "
                                   "named " moldname " in the namespaces "
                                   (s/join ", " @mold-namespaces))})
          :else 
          (throw-error {:mold (str "Couldn't find mold! Found these: "
                                   (s/join ", " finds) ", but none of them "
                                   "looked were molds")}))))))


(defn >NSMoldFinder [& mold-namespaces]
  (->NSMoldFinder (atom mold-namespaces)))


