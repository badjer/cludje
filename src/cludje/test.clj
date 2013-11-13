(ns cludje.test
  (:use midje.sweet
        cludje.util
        cludje.pipeline 
        cludje.authenticate
        cludje.actionfind
        cludje.moldfind
        cludje.authorize
        cludje.datastore
        cludje.email
        cludje.run
        cludje.types
        cludje.log)
  (:import [midje.util.exceptions ICapturedThrowable])
  (:require [clj-http.client :as http]
            [clj-http.cookies :as cookies]
            [cheshire.core :as cheshire]))


(defn has-keys [& kees]
  "A midje checker that returns truthy if the thing
  being tested contains all the specified keys"
  (contains (zipmap kees (repeat anything))))

(defn just-keys [& kees]
  "A midje checker that returns truthy if the thing
  being tested contains only the specified keys"
  (just (zipmap kees (repeat anything))))

(defn has-item? [partial-item]
  "A midje checker that returns true if the thing
  being tested is a seq, and one of the things in it
  contains partial-item"
  (fn [xs]
    (let [checker-fn (contains partial-item)
          res (map checker-fn xs)]
      (some #{true} res))))

(defn just-item? [partial-item]
  "A midje checker that returns true if the thing
  being tested is a seq, and all the things in the seq
  contains partial-item"
  (fn [xs]
    (let [checker-fn (contains partial-item)
          res (map checker-fn xs)]
      (and (seq res) (every? #{true} res)))))




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
  (contains? x :__alerts))

(defn has-alert [typ re]
  "A midje checker that returns true if the thing
  being tested is a cludje response that has an alert
  of typ with text that matches re"
  (fn [x]
    (when-let [alerts (:__alerts x)]
      (some #(and (= (name typ) (name (:type %)))
                  (re-find re (:text %)))
            alerts))))
         

(defn throws-404 [] 
  "Check that the fn throws a not found exception"
  (throws clojure.lang.ExceptionInfo #"^Not found"))
(defn throws-401 [] 
  "Check that the fn throws a not logged in exception"
  (throws clojure.lang.ExceptionInfo #"^Not logged in"))
(defn throws-403 [] 
  "Check that the fn throws a forbidden exception"
  (throws clojure.lang.ExceptionInfo #"^Unauthorized"))
(defn throws-problems []
  "Check that the fn throws problems"
  (throws clojure.lang.ExceptionInfo #"^Problems"))
(defn throws-error []
  "Check that the fn throws errors"
  (throws clojure.lang.ExceptionInfo #"^System error"))

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
    (assoc s :body (cheshire/parse-string (:body s) true))
    (cheshire/parse-string s true)))

(defn body [data]
  "Midje checker that checks if a response body is data"
  (fn [x]
    (= data (:body x))))

(defn status [code]
  "Midje checker that checks if a response status is code"
  (fn [x]
    (= code (:status x))))


(defn >cookies []
  (cookies/cookie-store))

(defn do-request [{:keys [url body method cookies] :or {url "http://google.ca" 
                                                        body "" 
                                                        cookies (>cookies) 
                                                        method :get}}]
  (case method 
    :get (http/get url {:throw-exceptions false :cookie-store cookies})
    :get-json (<-json (http/get url {:throw-exceptions false :cookie-store cookies}))
    :json (<-json (http/post url {:form-params body :content-type :json 
                                  :throw-exceptions false
                                  :cookie-store cookies}))))


(defn >request [input & request-parts]
  (-> (apply merge request-parts)
      (with-input input)))

(defn run ([action input & request-parts]
 (let [request (apply >request (conj request-parts input))]
   (-> (run-action action request)
       (:output)))))

(defn render [action input & request-parts]
  (let [request (apply >request (conj request-parts input))
        response (run-action action request)
        mold (get response :output-mold Anything)]
    (show mold (:output response))))


(defn >test-system [{:keys [action-namespaces mold-namespaces]}]
  {:authenticator (>TestAuthenticator)
   :action-finder (apply >NSActionFinder action-namespaces)
   :mold-finder (apply >NSMoldFinder mold-namespaces)
   :authorizer (>TestAuthorizer)
   :logger (>TestLogger)
   :data-store (>TestDatastore)
   :emailer (>TestEmailer)
   })
