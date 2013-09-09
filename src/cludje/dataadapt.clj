(ns cludje.dataadapt
  (:use cludje.system
        cludje.web
        cludje.util))

(defrecord TestDataAdapter []
  IDataAdapter
  (parse-input [self rawdata] rawdata)
  (render-output [self context] (?! context :molded-output)))

(defn >TestDataAdapter []
  (->TestDataAdapter))

(defrecord WebDataAdapter []
  IDataAdapter
  (parse-input [self rawdata] 
    (let [parsed (ring-parser rawdata)
          params (?! parsed :params)]
      params))
  (render-output [self context] 
    (json-respond context)))

(defn >WebDataAdapter []
  (->WebDataAdapter))

