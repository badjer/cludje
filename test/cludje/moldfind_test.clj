(ns cludje.moldfind-test
  (:use midje.sweet
        cludje.system
        cludje.test
        cludje.mold
        cludje.types
        cludje.moldfind)
  (:require [cludje.altnamespace :as ans]))


(fact "propose-moldname"
  (propose-moldnames :add-cog) => ["Cog" "cog"]
  (propose-moldnames :some-ns/add-cog) => ["Cog" "cog"]
  (propose-moldnames :foobar) => ["Foobar" "foobar"]
  (propose-moldnames :some-ns/foobar) => ["Foobar" "foobar"])

(defn >input [action-sym]
  {:action-sym action-sym})

(def mold (>Mold {:name Str} {}))
(def Cog (>Mold {:name Str} {}))
(def notmold 1)

(fact "NSMoldFinder"
  (let [mf (>NSMoldFinder 'cludje.moldfind-test 'cludje.altnamespace)]
    (fact "satisfies IMoldFinder"
      (satisfies? IMoldFinder mf) => true)
    (fact "finds a mold"
      (find-mold mf (>input :mold)) => `mold
      (fact "with different casing"
        (find-mold mf (>input :cog)) => `Cog
        (find-mold mf (>input "cog")) => `Cog))
    (fact "works with fully-qualified names"
      (find-mold mf (>input `mold)) => `mold)
    (fact "finds a mold in another namespace"
      (find-mold mf (>input :altnsmold)) => `ans/altnsmold)
    (fact "throws exception if the thing isn't a mold"
      (find-mold mf (>input :notmold)) => (throws-error))))
