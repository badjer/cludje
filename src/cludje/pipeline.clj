(ns cludje.pipeline
  (:use cludje.util
        cludje.types
        cludje.errors
        cludje.run
        cludje.system)
  (:require [clojure.stacktrace :as stack]
            [clojure.pprint :as pprint]))

(defn with-params 
  ([params]
   (with-params {} params))
  ([request params]
   (assoc request :params params)))

(defn with-session 
  ([session-map]
   (with-session {} session-map))
  ([request session-map]
   (assoc request :session session-map)))


(defn with-system 
  ([system]
   (with-system {} system))
  ([request system]
    (assoc request :system system)))

(defn in-system [pipeline system]
  (fn [request]
    (-> request
        (with-system system)
        (pipeline))))


(defn as-user 
  ([user]
   (as-user {} user))
  ([request user]
    (assoc request :user user)))

(defn add-authenticate [pipeline]
  (fn [request]
    (let [authenticator (?! request [:system :authenticator])
          user (current-user authenticator request)]
      (-> request
          (as-user user)
          (pipeline)))))


(defn with-action 
  ([action]
   (with-action {} action))
  ([request action]
    (assoc request :action action)))

(defn add-action [pipeline]
  (fn [request]
    (let [finder (?! request [:system :action-finder])
          action (find-action finder request)]
      (-> request
          (with-action action)
          (pipeline)))))

(defn with-input 
  ([input]
   (with-input {} input))
  ([request input]
    (assoc request :input input)))

(defn add-input [pipeline]
  (fn [request]
    (let [input (?! request :params)]
      (-> request
          (with-input input)
          (pipeline)))))

(defn authorize [pipeline]
  (fn [request]
    (let [authorizer (?! request [:system :authorizer])
          ok? (can? authorizer request)]
      (if-not ok?
        (throw-unauthorized)
        (pipeline request)))))

(defn- handleable-exception? [ex]
  (let [exd (ex-data ex)]
    (cond
      (:__notfound exd) true
      (:__notloggedin exd) true
      (:__unauthorized exd) true
      :else false)))

(defn- log-exception [request ex]
  (let [logger (? request [:system :logger])
        data-str (with-out-str (pprint/pprint (ex-data ex)))
        request-str (with-out-str (pprint/pprint request))
        trace-str (with-out-str (stack/print-stack-trace ex))]
    (log logger
         (str "Error!\n" ex "\n\nData:\n" data-str
              "\n\nRequest:\n" request-str
              "\n\nStacktrace:\n" trace-str))))

(defn- throw-exception [request ex]
  (when (not (handleable-exception? ex))
    (log-exception request ex))
  (throw ex))

(defn log-exceptions [pipeline]
  (fn [request]
    (try
      (pipeline request)
      (catch java.lang.Exception ex
        (throw-exception request ex)))))

(defn with-output 
  ([output]
   (with-output {} output))
  ([request output]
    (assoc request :output output)))

(defn add-output [pipeline]
  (fn [request]
    (let [action (?! request :action)
          done-request (run-action action request)]
      (pipeline done-request))))

(defn with-output-mold 
  ([mold]
   (with-output-mold {} mold))
  ([request mold]
    (assoc request :output-mold mold)))

(defn add-output-mold [pipeline]
  (fn [request]
    (let [done-request (pipeline request)]
      (if (:output-mold done-request)
        done-request
        (let [moldfinder (?! done-request [:system :mold-finder]) 
              output-mold (find-output-mold moldfinder done-request)] 
          (-> done-request
              (with-output-mold output-mold)))))))

(defn with-result 
  ([result]
   (with-result {} result))
  ([request result]
    (assoc request :result result)))

(defn add-result [pipeline]
  (fn [request]
    (let [done-request (pipeline request)
          output-mold (?! done-request :output-mold)
          output (?! done-request :output)
          result (show output-mold output)]
      (-> done-request
          (with-result result)))))


(def api-pipeline
  (-> identity
      (add-output)
      (add-output-mold)
      (add-result)
      (authorize)
      (add-input)
      (add-action)
      (add-authenticate)
      (log-exceptions)))

