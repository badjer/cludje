(ns cludje.dataadapt
  (:use cludje.system
        cludje.web
        cludje.util))

(defrecord TestDataAdapter []
  IDataAdapter
  (parse-input [self context]
    (assoc context :parsed-input (?! context :raw-input)))
  (render-output [self context] (?! context :molded-output)))

(defn >TestDataAdapter []
  (->TestDataAdapter))

(defrecord WebDataAdapter []
  IDataAdapter
  (parse-input [self context] 
    (let [parsed (ring-parser (?! context :raw-input))
          params (?! parsed :params)]
      (println "RING PARSED IS" parsed)
      (-> context
          (assoc :parsed-input params)
          (assoc :ring-request parsed))))
  (render-output [self context] 
    (json-respond context)))

(defn >WebDataAdapter []
  (->WebDataAdapter))

