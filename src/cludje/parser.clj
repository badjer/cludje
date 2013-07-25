(ns cludje.parser
  (:require [clojure.string :as s])
  (:use cludje.core
        cludje.types))

(defn is-api-call? [allow-get? request]
  (let [postcheck (or allow-get? (= (:request-method request) :post))
        uri-check (= "/api" (:uri request))]
    (and postcheck uri-check)))

(defn is-transient-field [field]
  (re-find #"^__" (name field)))

(defn- strip-transient-fields [input]
  "Strip off any fields that start with __"
  (let [victims (filter is-transient-field (keys input))]
    (apply dissoc input victims)))

(defn is-persistent-field [field]
  (re-find #"^_p_" (name field)))

(defn- assoc-cookies [input request]
  "Read the persistent fields out of the cookie and set them to the input"
  (let [fields (filter is-persistent-field (keys (:cookies request)))]
    (apply merge input
           (for [f fields]
             {(keyword f) (get-in request [:cookies f :value])}))))

(defn- cleanup-input [input request]
  (-> input
      (strip-transient-fields)
      (assoc-cookies request)))

(defrecord WebInputParser [allow-api-get?]
  IInputParser
  (parse-input- [self request]
    (let [data (get request :params (get request :body))]
      (when (is-api-call? allow-api-get? request)
        (cleanup-input data request)))))

(defn make-webinputparser [opts]
  (->WebInputParser (get opts :allow-api-get? false)))


