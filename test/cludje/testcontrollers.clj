(ns cludje.testcontrollers
  (:use cludje.core
        cludje.types))

(defmodel Foo {:bar Str})

(defability ab-add-foo
  :add Foo true)

(defability ab-remove-foo
  :remove Foo true)

(defn not-an-action [] 5)

(defaction foo-index input)
