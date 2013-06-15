(ns cludje.models
  (:use cludje.types
        cludje.core))

(defmodel User {:email Email :password Password})

;(defmodel User 
  ;{:fields {:email Email :password Password}})
