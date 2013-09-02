(ns cludje.dataadapt
  (:use cludje.system))

(defrecord TestDataAdapter []
  IDataAdapter
  (parse-input [self rawdata] rawdata)
  (render-output [self output] output))

(defn >TestDataAdapter []
  (->TestDataAdapter))

