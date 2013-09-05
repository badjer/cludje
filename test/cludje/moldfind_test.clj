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

(def mold (>Mold {:name Str} {}))
(def Cog (>Mold {:name Str} {}))
(def notmold 1)

(def foo-bar-input (>Mold {:name Str :amt Int} {}))
(def foo-bar-output (>Mold {:amt Int} {}))

(defn test-find [f mf]
  (fact "finds a mold"
    (f mf (>input :mold)) => `mold
    (fact "with different casing"
      (f mf (>input :cog)) => `Cog
      (f mf (>input "cog")) => `Cog))
  (fact "works with fully-qualified names"
    (f mf (>input `mold)) => `mold)
  (fact "finds a mold in another namespace"
    (f mf (>input :altnsmold)) => `ans/altnsmold)
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
        (find-input-mold mf (>input :foo-bar)) => `foo-bar-input))
    (fact "find-output-mold"
      (test-find find-output-mold mf)
      (fact "finds <action-name>-output first"
        (find-output-mold mf (>input :foo-bar)) => `foo-bar-output))))

