(ns cludje.moldfind
  (:use cludje.system
        cludje.mold
        cludje.types
        cludje.util
        cludje.errors
        cludje.find)
  (:require [clojure.string :as s]))

(defn- add-output-fields [mold]
  (>Mold {:_ mold :__problems Anything} {}))

(defrecord SingleMoldFinder [mold]
  IMoldFinder
  (find-output-mold [self request] (add-output-fields mold)))

(defn >SingleMoldFinder [mold]
  (->SingleMoldFinder mold))

(defn sym-to-mold [sym]
  (let [m @(resolve sym)]
    (when (satisfies? IMold m)
      m)))


(defn propose-output-moldnames [action-name]
  (let [action (str (name action-name))
        ; We assume that the action is of the form:
        ; some-ns/operation-model
        unqualified-act (last (s/split action #"/"))
        output-mold (str unqualified-act "-output")
        modelname (last (s/split unqualified-act #"-"))
        uppered (s/capitalize modelname)]
    [output-mold uppered modelname]))

(defn- find-mold- [mold-namespaces mold-names]
  (let [finds (search-in-nses mold-namespaces mold-names)
        matches (keep sym-to-mold finds)]
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
  (find-output-mold [self request]
    (let [names (propose-output-moldnames (?! request [:params :_action]))
          mold (find-mold- @mold-namespaces names)
          res (add-output-fields mold)]
      res)))


(defn >NSMoldFinder [& mold-namespaces]
  (->NSMoldFinder (atom mold-namespaces)))


