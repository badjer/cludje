(ns cludje.moldfind-test
  (:use midje.sweet
        cludje.system
        cludje.test
        cludje.mold
        cludje.types
        cludje.moldfind)
  (:require [cludje.altnamespace :as ans]))


(fact "propose-input-moldnames"
  (propose-input-moldnames :add-cog) => ["add-cog-input" "Cog" "cog"]
  (propose-input-moldnames :some-ns/add-cog) => ["add-cog-input" "Cog" "cog"]
  (propose-input-moldnames :foobar) => ["foobar-input" "Foobar" "foobar"]
  (propose-input-moldnames :some-ns/foobar) => ["foobar-input" "Foobar" "foobar"])

(fact "propose-output-moldnames"
  (propose-output-moldnames :add-cog) => ["add-cog-output" "Cog" "cog"]
  (propose-output-moldnames :some-ns/add-cog) => ["add-cog-output" "Cog" "cog"]
  (propose-output-moldnames :foobar) => ["foobar-output" "Foobar" "foobar"]
  (propose-output-moldnames :some-ns/foobar) => ["foobar-output" "Foobar" "foobar"])

(defn >input [action-sym]
  {:action-sym action-sym})

(def mold-fields {:name Str})
(def mold (>Mold mold-fields {}))
(def cog-fields {:cogname Str})
(def Cog (>Mold cog-fields {}))
(def notmold 1)

(def foo-bar-input-fields {:name Str :amt Int})
(def foo-bar-input (>Mold foo-bar-input-fields {}))
(def foo-bar-output-fields {:amt Int})
(def foo-bar-output (>Mold foo-bar-output-fields {}))

(defn test-find [f mf]
  (fact "finds a mold"
    (fields (f mf (>input :mold))) => (contains (fields mold))
    (fact "with different casing"
      (fields (f mf (>input :cog))) => (contains (fields Cog))
      (fields (f mf (>input "cog"))) => (contains (fields Cog))))
  (fact "works with fully-qualified names"
    (fields (f mf (>input `mold))) => (contains (fields mold)))
  (fact "finds a mold in another namespace"
    (fields (f mf (>input :altnsmold))) => (contains (fields ans/altnsmold)))
  (fact "throws exception if _action not supplied"
    (f mf (>input nil)) => (throws-error))
  (fact "throws exception if can't find"
    (f mf (>input :random-func)) => (throws-error))
  (fact "throws exception if the thing isn't a mold"
    (f mf (>input :notmold)) => (throws-error)))

(fact "NSMoldFinder"
  (let [mf (>NSMoldFinder 'cludje.moldfind-test 'cludje.altnamespace)]
    (fact "satisfies IMoldFinder"
      (satisfies? IMoldFinder mf) => true)
    (fact "find-input-mold"
      (test-find find-input-mold mf)
      (fact "Finds <action-name>-input first"
        (find-input-mold mf (>input :foo-bar)) => foo-bar-input))
    (fact "find-output-mold"
      (test-find find-output-mold mf)
      (fact "finds <action-name>-output first"
        (fields (find-output-mold mf (>input :foo-bar))) => 
          (contains (fields foo-bar-output)))
      (fact "attaches :__problems Anything to the mold it returns"
        (fields (find-output-mold mf (>input :foo-bar))) => 
          (contains {:__problems Anything})))))

