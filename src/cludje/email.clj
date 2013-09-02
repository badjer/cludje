(ns cludje.email
  (:use cludje.system))

(defrecord TestEmailer [messages]
  IEmailer
  (send-mailmessage [self message]
    (swap! messages conj message)))

(defn >TestEmailer []
  (->TestEmailer (atom [])))


(defrecord LogEmailer [logger]
  IEmailer
  (send-mailmessage [self message] (log logger message)))
