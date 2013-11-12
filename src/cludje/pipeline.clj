(ns cludje.pipeline
  (:use cludje.util
        cludje.types
        cludje.errors
        cludje.run
        cludje.system))

(defn with-system [request system]
  (assoc request :system system))

(defn in-system [pipeline system]
  (fn [request]
    (-> request
        (with-system system)
        (pipeline))))


(defn as-user [request user]
  (assoc request :user user))

(defn add-authenticate [pipeline]
  (fn [request]
    (let [authenticator (?! request [:system :authenticator])
          user (current-user authenticator request)]
      (-> request
          (as-user user)
          (pipeline)))))


(defn with-action [request action]
  (assoc request :action action))

(defn add-action [pipeline]
  (fn [request]
    (let [finder (?! request [:system :action-finder])
          action (find-action finder request)]
      (-> request
          (with-action action)
          (pipeline)))))

(defn with-input [request input]
  (assoc request :input input))

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

(defn with-output [request output]
  (assoc request :output output))

(defn add-output [pipeline]
  (fn [request]
    (let [action (?! request :action)
          done-request (run-action action request)]
      (pipeline done-request))))

(defn with-output-mold [request mold]
  (assoc request :output-mold mold))

(defn add-output-mold [pipeline]
  (fn [request]
    (let [done-request (pipeline request)]
      (if (:output-mold done-request)
        done-request
        (let [moldfinder (?! done-request [:system :mold-finder]) 
              output-mold (find-output-mold moldfinder done-request)] 
          (-> done-request
              (with-output-mold output-mold)))))))

(defn with-result [request result]
  (assoc request :result result))

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
      (add-authenticate)))

