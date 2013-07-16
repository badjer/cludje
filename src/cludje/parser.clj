(ns cludje.parser
  (:use cludje.core
        cludje.types))

(defn- is-api-call? [allow-get? request]
  (let [postcheck (or allow-get? (= (:request-method request) :post))
        uri-check (= "/api" (:uri request))]
    (and postcheck uri-check)))

(defrecord WebInputParser [allow-api-get?]
  IInputParser
  (parse-input- [self request]
    (let [data (get request :params (get request :body))]
      (when (is-api-call? allow-api-get? request)
        data))))

(defn make-webinputparser [opts]
  (->WebInputParser (get opts :allow-api-get? false)))


