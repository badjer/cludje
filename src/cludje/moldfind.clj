(ns cludje.moldfind
  (:use cludje.system
        cludje.mold
        cludje.util
        cludje.errors
        cludje.find)
  (:require [clojure.string :as s]))

(defrecord SingleMoldFinder [mold-sym]
  IMoldFinder
  (find-input-mold [self context] mold-sym)
  (find-output-mold [self context] mold-sym))

(defn >SingleMoldFinder [mold-sym]
  (->SingleMoldFinder mold-sym))

(defn is-mold? [sym]
  (let [m @(resolve sym)]
    (satisfies? IMold m)))


(defn propose-input-moldnames [action-sym]
  (let [action (str (name action-sym))
        ; We assume that the action is of the form:
        ; some-ns/operation-model
        unqualified-act (last (s/split action #"/"))
        input-mold (str unqualified-act "-input")
        modelname (last (s/split unqualified-act #"-"))
        uppered (s/capitalize modelname)]
    [input-mold uppered modelname]))

(defn propose-output-moldnames [action-sym]
  (let [action (str (name action-sym))
        ; We assume that the action is of the form:
        ; some-ns/operation-model
        unqualified-act (last (s/split action #"/"))
        output-mold (str unqualified-act "-output")
        modelname (last (s/split unqualified-act #"-"))
        uppered (s/capitalize modelname)]
    [output-mold uppered modelname]))

(defn- find-mold- [mold-namespaces mold-names]
  (let [finds (search-in-nses mold-namespaces mold-names)
        matches (filter is-mold? finds)]
      (if (seq matches)
        (first matches)
        (cond 
          (empty? finds)
          (throw-error {:mold (str "Couldn't find mold! Couldn't find anything "
                                   "named " mold-names " in the namespaces "
                                   (s/join ", " mold-namespaces))})
          :else 
          (throw-error {:mold (str "Couldn't find mold! Found these: "
                                   (s/join ", " finds) ", but none of them "
                                   "looked were molds")})))))


(defrecord NSMoldFinder [mold-namespaces]
  IMoldFinder
  (find-input-mold [self context]
    (find-mold- @mold-namespaces (propose-input-moldnames (?! context :action-sym))))
  (find-output-mold [self context]
    (find-mold- @mold-namespaces (propose-output-moldnames (?! context :action-sym)))))


(defn >NSMoldFinder [& mold-namespaces]
  (->NSMoldFinder (atom mold-namespaces)))


