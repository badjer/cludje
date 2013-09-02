(ns cludje.moldfind-test
  (:use midje.sweet
        cludje.system
        cludje.test
        cludje.mold
        cludje.types
        cludje.moldfind)
  (:require [cludje.altnamespace :as ans]))


(fact "propose-moldname"
  (propose-moldname :add-cog) => "cog"
  (propose-moldname :some-ns/add-cog) => "cog"
  (propose-moldname :foobar) => "foobar"
  (propose-moldname :some-ns/foobar) => "foobar")

(defn >input [action-sym]
  {:action-sym action-sym})

(def mold (>Mold {:name Str} {}))
(def notmold 1)

(fact "NSMoldFinder"
  (let [mf (>NSMoldFinder 'cludje.moldfind-test 'cludje.altnamespace)]
    (fact "satisfies IMoldFinder"
      (satisfies? IMoldFinder mf) => true)
    (fact "finds a mold"
      (find-mold mf (>input :mold)) => `mold)
    (fact "works with fully-qualified names"
      (find-mold mf (>input `mold)) => `mold)
    (fact "finds a mold in another namespace"
      (find-mold mf (>input :altnsmold)) => `ans/altnsmold)
    (fact "throws exception if the thing isn't a mold"
      (find-mold mf (>input :notmold)) => (throws-error))))
