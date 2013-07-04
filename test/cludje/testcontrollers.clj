(ns cludje.testcontrollers
  (:use cludje.core
        cludje.types))

(defmodel Foo {:bar Str})

(defability ab-foo
  :add Foo true)

(defn not-an-action [] 5)

(defaction index input)
