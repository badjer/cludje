(ns cludje.demo.actions
  (:use cludje.types
        cludje.core
        cludje.crud
        cludje.demo.models))

(def-crud-actions Cog)
