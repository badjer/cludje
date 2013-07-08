(ns cludje.test
  (:use midje.sweet)
  (:require [clj-http.client :as http]
            [cheshire.core :as cheshire]))

(defn has-keys [& kees]
  "A midje checker that returns truthy if the thing
  being tested contains all the specified keys"
  (contains (zipmap kees (repeat anything))))

(defn just-keys [& kees]
  "A midje checker that returns truthy if the thing
  being tested contains only the specified keys"
  (just (zipmap kees (repeat anything))))

(defn has-problems? [x]
  "A midje checker that returns true if the thing
  being tested is a cludje response that has problems"
  (contains? x :problems))

(defn has-problems [& kees]
  "A midje checker that returns true if the thing
  being tested is a cludje response that has problems
  that contain the specified keys"
  (fn [x] 
    (and (contains? x :problems)
         ((apply has-keys kees) (:problems x)))))

(defn ->json [x]
  (cheshire/generate-string x))

(defn <-json [s]
  (cheshire/parse-string s true))


(defn do-request [{:keys [url body method] :or {url "http://google.ca"
                                                body ""
                                                method :get}}]
  (case method 
    :get (http/get url)
    :json (<-json (:body (http/post url {:form-params body :content-type :json})))))
