(ns cludje.mailer
  (:use cludje.core))

(defrecord MemMailer [mailatom]
  IMailer
  (send-mail- [self message]
    (swap! mailatom conj message)))

(defrecord LogMailer [logger]
  IMailer
  (send-mail- [self message] (log- logger message)))
