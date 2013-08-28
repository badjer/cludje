(ns cludje.email
  (:use cludje.system))

(defrecord TestEmailer [mails]
  IEmailer
  (send-mailmessage [self message]
    (swap! mails conj message)))

(defn >TestEmailer []
  (->TestEmailer (atom [])))


(defrecord LogEmailer [logger]
  IEmailer
  (send-mailmessage [self message] (log logger message)))
