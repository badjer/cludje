(ns cludje.app
  (:use cludje.core
        cludje.types
        cludje.system))

(defmodel User {:email Email :password Password :name Str}
  :require [:email :password])


