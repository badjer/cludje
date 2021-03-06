(ns cludje.moldfind-test
  (:use midje.sweet
        cludje.system
        cludje.test
        cludje.mold
        cludje.types
        cludje.moldfind)
  (:require [cludje.altnamespace :as ans]))


(fact "propose-output-moldnames"
  (propose-output-moldnames :add-cog) => ["add-cog-output" "Cog" "cog"]
  (propose-output-moldnames :some-ns/add-cog) => ["add-cog-output" "Cog" "cog"]
  (propose-output-moldnames :foobar) => ["foobar-output" "Foobar" "foobar"]
  (propose-output-moldnames :some-ns/foobar) => ["foobar-output" "Foobar" "foobar"])

(defn >input [action]
  {:params {:_action action}})

(def mold-fields {:name Str})
(defmold mold mold-fields)
(def cog-fields {:cogname Str})
(defmold Cog cog-fields)
(def notmold 1)

(def foo-bar-output-fields {:amt Int})
(defmold foo-bar-output foo-bar-output-fields)

(defn test-find [f mf]
  (fact "finds a mold"
    (fields (f mf (>input :mold))) => (contains (fields mold))
    (fact "with different casing"
      (fields (f mf (>input :cog))) => (contains (fields Cog))
      (fields (f mf (>input "cog"))) => (contains (fields Cog))))
  (fact "works with fully-qualified names"
    (fields (f mf (>input "cludje.moldfind-test/mold"))) => (contains (fields mold)))
  (fact "finds a mold in another namespace"
    (fields (f mf (>input :altnsmold))) => (contains (fields ans/altnsmold)))
  (fact "throws exception if _action not supplied"
    (f mf (>input nil)) => (throws-error))
  (fact "returns Anything if can't find"
    (f mf (>input :random-func)) => Anything)
  (fact "throws exception if the thing isn't a mold"
    (f mf (>input :notmold)) => (throws-error)))

(fact "NSMoldFinder"
  (let [mf (>NSMoldFinder 'cludje.moldfind-test 'cludje.altnamespace)]
    (fact "satisfies IMoldFinder"
      (satisfies? IMoldFinder mf) => true)
    (fact "find-output-mold"
      (test-find find-output-mold mf)
      (fact "finds <action-name>-output first"
        (fields (find-output-mold mf (>input :foo-bar))) => 
          (contains (fields foo-bar-output)))
      (fact "mold allows __ fields to be shown"
        (let [mold (find-output-mold mf (>input :foo-bar))]
          (show mold {:__problems :foo}) => {:__problems :foo}))
      )
    ))
