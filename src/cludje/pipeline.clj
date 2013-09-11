(ns cludje.pipeline
  (:use cludje.util
        cludje.types
        cludje.errors
        cludje.run
        cludje.system))

(defn in-system [pipeline system]
  (fn [request]
    (-> request
        (assoc :system system)
        (pipeline))))





(defn add-authenticate [pipeline]
  (fn [request]
    (let [authenticator (?! request [:system :authenticator])
          user (current-user authenticator request)]
      (-> request
          (assoc :user user)
          (pipeline)))))

(defn add-action [pipeline]
  (fn [request]
    (let [finder (?! request [:system :action-finder])
          action (find-action finder request)]
      (-> request
          (assoc :action action)
          (pipeline)))))

(defn add-input [pipeline]
  (fn [request]
    (let [params (?! request :params)]
      (-> request
          (assoc :input params)
          (pipeline)))))

(defn authorize [pipeline]
  (fn [request]
    (let [authorizer (?! request [:system :authorizer])
          ok? (can? authorizer request)]
      (if-not ok?
        (throw-unauthorized)
        (pipeline request)))))

(defn add-output [pipeline]
  (fn [request]
    (let [action (?! request :action)
          done-request (run-action action request)]
      (pipeline done-request))))

(defn add-output-mold [pipeline]
  (fn [request]
    (let [done-request (pipeline request)]
      (if (:output-mold done-request)
        done-request
        (let [moldfinder (?! done-request [:system :mold-finder]) 
              output-mold (find-output-mold moldfinder done-request)] 
          (assoc done-request :output-mold output-mold))))))

(defn add-result [pipeline]
  (fn [request]
    (let [done-request (pipeline request)
          output-mold (?! done-request :output-mold)
          output (?! done-request :output)
          prepared (show output-mold output)]
      (assoc done-request :result prepared))))

(def api-pipeline
  (-> identity
      (add-output)
      (add-output-mold)
      (add-result)
      (authorize)
      (add-input)
      (add-action)
      (add-authenticate)))

