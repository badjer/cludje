(ns cludje.pipeline
  (:use cludje.util
        cludje.system))

(defn add-session [{:keys [system rawdata]}])

(defn authenticate [{:keys [system rawdata session] :as context}])

(defn resolve-action [{:keys [system rawdata session user] :as context}])

(defn authorize [{:keys [system rawdata session action user] :as context}])

(defn resolve-input-mold [{:keys [system rawdata action user] :as context}])

(defn parse-data [{:keys [system rawdata action user input-mold] :as context}])

(defn run-action [{:keys [system rawdata action user input-mold data]}])

(defn resolve-output-format [{:keys [system rawdata action user 
                                     input-mold data output] :as context}])

(defn render-output [{:keys [system rawdata action user
                             input-mold data output output-mold] :as context}])
(defn persist-session [context])

(defn with-session [{:keys [system rawdata]}])
(defn with-authenticate [{:keys [system rawdata session]}])
(defn with-action [{:keys [system rawdata session user]}])
(defn with-authorize [{:keys [system rawdata session user action]}])
(defn with-input-mold [{:keys [system rawdata session user action]}])
(defn with-data [{:keys [system rawdata session user action input-mold]}])
(defn with-output [{:keys [system rawdata session user action input-mold 
                           data]}])
(defn with-output-format [{:keys [system rawdata session user action 
                                  input-mold data output]}])
(defn with-rendered-output [{:keys [system rawdata session user action
                                    input-mold data output output-mold]}])
(defn persist-session [context])


(defn wrap-context [f]
  (fn [rawinput]
    (-> {:rawinput rawinput}
        (f))))
(defn wrap-system [f system]
  (fn [context]
    (-> context
        (assoc :system system)
        (f))))
(defn wrap-session [f]
  (fn [context]
    (-> context
        (assoc :session nil)
        (f)
        (persist-session))))
(defn wrap-authenticate [f]
  (fn [context]
    (let [authenticator (? context [:system :authenticator])
          user (current-user authenticator context)]
      (-> context
          (assoc :user user)
          (f)))))
(defn wrap-action [f]
  (fn [context]
    (-> context
        (assoc :action nil)
        (f))))
(defn wrap-authorize [f]
  (fn [context]
    (-> context
        ; Throw exception
        (assoc :authorized? nil)
        (f))))
(defn wrap-molds [f]
  (fn [context]
    (-> context
        (assoc :input-mold nil)
        (f)
        (assoc :output-mold nil))))
(defn wrap-data [f]
  (fn [context]
    (-> context
        (assoc :input nil)
        (f)
        (assoc :rawoutput nil))))
(defn wrap-output [f]
  (fn [context]
    (let [output ((:action context) (:system context) (:input context))]
      (-> context
          (assoc :output output)
          (f)))))

