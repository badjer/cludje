(ns cludje.pipeline
  (:use cludje.util
        cludje.types
        cludje.errors
        cludje.run
        cludje.system))

(defn add-system [f system]
  (fn [request]
    (-> request
        (assoc :system system)
        (f))))

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

(defn add-input [f]
  (fn [request]
    (let [params (?! request :params)]
      (-> request
          (assoc :input params)
          (f)))))

(defn authorize [f]
  (fn [request]
    (let [authorizer (?! request [:system :authorizer])
          ok? (can? authorizer request)]
      (if-not ok?
        (throw-unauthorized)
        (f request)))))

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
