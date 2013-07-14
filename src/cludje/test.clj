(ns cludje.test
  (:use midje.sweet)
  (:require [clj-http.client :as http]
            [cheshire.core :as cheshire]
            [cludje.server :as server]
            [cludje.app :as app]))

(defn test-system []
  (app/make-system {:server (server/->MockServer)}))

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
  (contains? x :__problems))

(defn has-problems [& kees]
  "A midje checker that returns true if the thing
  being tested is a cludje response that has problems
  that contain the specified keys"
  (fn [x] 
    (and (contains? x :__problems)
         ((apply has-keys kees) (:__problems x)))))

(defn ->json [x]
  (cheshire/generate-string x))

(defn <-json [s]
  (if (:body s)
    (recur (:body s))
    (cheshire/parse-string s true)))

(defn do-request [{:keys [url body method] :or {url "http://google.ca"
                                                body ""
                                                method :get}}]
  (case method 
    :get (http/get url)
    :json (<-json (:body (http/post url {:form-params body :content-type :json})))))

