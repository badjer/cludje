(ns cludje.pipeline
  (:use cludje.util
        cludje.types
        cludje.errors
        cludje.system)
  (:require [clojure.set :as st]))


(defn add-system [f system]
  (fn [request]
    (-> request
        (assoc :system system)
        (f))))

;(defn wrap-parsed-input [f]
  ;(fn [request]
    ;(let [adapter (?! request [:system :data-adapter])]
      ;(->> request
           ;(parse-input adapter)
           ;(f)))))

;(defn wrap-session [f]
  ;(fn [request]
    ;(let [session-store (?! request [:system :session-store])]
      ;(->> request 
           ;(add-session session-store)
           ;(f)
           ;(persist-session session-store)))))
      ;(persist-session session-store (f (add-session session-store request))))))
          ;in-session (current-session session-store request)
          ;input (assoc request :session in-session)
          ;;output (f input)]
      ;(persist-session session-store output))))


(defn add-authenticate [f]
  (fn [request]
    (let [authenticator (?! request [:system :authenticator])
          user (current-user authenticator request)]
      (-> request
          (assoc :user user)
          (f)))))

(defn add-action [f]
  (fn [request]
    (let [finder (?! request [:system :action-finder])
          action (find-action finder request)]
      (-> request
          (assoc :action-sym action)
          (f)))))

(defn add-input-mold [f]
  (fn [request]
    (let [moldfinder (?! request [:system :mold-finder])
          input-mold (find-input-mold moldfinder request)]
      (-> request
          (assoc :input-mold input-mold)
          (f)))))

(defn add-input [f]
  (fn [request]
    (let [input-mold (?! request :input-mold)
          params (?! request :params)
          input (parse input-mold params)]
      (-> request
          (assoc :input input)
          (f)))))

(defn authorize [f]
  (fn [request]
    (let [authorizer (?! request [:system :authorizer])
          ok? (can? authorizer request)]
      (if-not ok?
        (throw-unauthorized)
        (f request)))))

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

(defn add-output [f]
  (fn [request]
    (let [action-sym (?! request :action-sym)
          action (resolve action-sym)
          done-request (run-action action request)]
      (f done-request))))

(defn add-output-mold [f]
  (fn [request]
    (let [done-request (f request)]
      (if (:output-mold done-request)
        done-request
        (let [moldfinder (?! done-request [:system :mold-finder]) 
              output-mold (find-output-mold moldfinder done-request)] 
          (assoc done-request :output-mold output-mold))))))

(defn add-result [f]
  (fn [request]
    (let [done-request (f request)
          output-mold (?! done-request :output-mold)
          output (?! done-request :output)
          prepared (show output-mold output)]
      (assoc done-request :result prepared))))


;(defn wrap-rendered-output [f]
  ;(fn [request]
    ;(let [done-request (f request)
          ;;data-adapter (?! done-request [:system :data-adapter])
          ;rendered (render-output data-adapter done-request)]
      ;(update-in done-request [:rendered-output] merge rendered))))


; Pipeline constructor functions
;(defn wrap-request [f]
  ;(fn [raw-input]
    ;(f {:raw-input raw-input})))

;(defn unwrap-request [f selector]
  ;(fn [request]
    ;(let [done-request (f request)
          ;res (selector done-request)]
      ;(if-not (empty? res)
        ;res
        ;(throw-error {:request (dissoc done-request :system)})))))


;(defn >pipeline [f]
  ;"Loads the input into a request, and extracts the output from the request"
  ;(-> f
      ;(wrap-request)
      ;(unwrap-request :rendered-output)))
