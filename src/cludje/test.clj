(ns cludje.test
  (:use midje.sweet
        cludje.core)
  (:import [midje.util.exceptions ICapturedThrowable])
  (:require [clj-http.client :as http]
            [cheshire.core :as cheshire]
            [cludje.server :as server]
            [cludje.login :as login]
            [cludje.logger :as logger]
            [cludje.app :as app]))

(defn test-system 
  ([] (test-system nil))
  ([cur-user]
   (let [lgin (login/make-TestLogin cur-user)]
     (app/make-system {:server (server/->MockServer)
                       :logger (logger/->MemLogger (atom []))
                       :login lgin}))))


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

(defn has-alerts? [x]
  "A midje checker that returns true if the thing
  being tested is a cludje response that has a alerts"
  (contains? x :_alerts))

(defn has-alert [typ re]
  "A midje checker that returns true if the thing
  being tested is a cludje response that has an alert
  of typ with text that matches re"
  (fn [x]
    (let [x-msg (get-in x [:_alerts 0 :text])
          x-typ (get-in x [:_alerts 0 :type])]
      (and (contains? x :_alerts)
           (= (name typ) (name x-typ))
           (re-find re x-msg)))))
         

(defn throws-404 [] 
  "Check that the fn throws a not found exception"
  (throws clojure.lang.ExceptionInfo "Not found"))
(defn throws-401 [] 
  "Check that the fn throws a not logged in exception"
  (throws clojure.lang.ExceptionInfo "Not logged in"))
(defn throws-403 [] 
  "Check that the fn throws a forbidden exception"
  (throws clojure.lang.ExceptionInfo "Unauthorized"))

(defn ok? [x]
  "A midje checker that makes sure there's no exception, and that
  there are no problems"
  (cond
    (map? x) (not (contains? x :__problems))
    (instance? ICapturedThrowable x) false
    :else true))

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

