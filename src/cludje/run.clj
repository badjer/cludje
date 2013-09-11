(ns cludje.run
  (:require [clojure.set :as st]))

(defn looks-like-response? [output request]
  ; If it's got all the same keys as request, 
  ; and it has :output
  ; it's a response
  (and (st/subset? (set (keys request)) (set (keys output)))
       (contains? output :output)))

(defn >response-map [output request]
  (if (looks-like-response? output request)
    output
    (assoc request :output output)))

(defn run-action [action request]
  (try
    (let [output (action request)]
      (>response-map output request))
    (catch clojure.lang.ExceptionInfo ex
      (let [exd (ex-data ex)]
        (if (:__problems exd)
          (assoc request :output (merge (:input request) exd))
          (throw ex))))))


