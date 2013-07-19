(ns cludje.parser
  (:require [clojure.string :as s])
  (:use cludje.core
        cludje.types))

(defn- is-api-call? [allow-get? request]
  (let [postcheck (or allow-get? (= (:request-method request) :post))
        uri-check (= "/api" (:uri request))]
    (and postcheck uri-check)))

(defn- is-transient-field [field]
  (re-find #"^__" (name field)))

(defn- cleanup-input [input]
  "Strip off any fields that start with __"
  (let [victims (filter is-transient-field (keys input))]
    (apply dissoc input victims)))

(defrecord WebInputParser [allow-api-get?]
  IInputParser
  (parse-input- [self request]
    (let [data (get request :params (get request :body))]
      (when (is-api-call? allow-api-get? request)
        (cleanup-input data)))))

(defn make-webinputparser [opts]
  (->WebInputParser (get opts :allow-api-get? false)))


