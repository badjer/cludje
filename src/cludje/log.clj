(ns cludje.log
  (:use cludje.system))

(defrecord TestLogger [entries]
  ILog
  (log [self message]
    (swap! entries conj message)))

(defn >TestLogger []
  (->TestLogger (atom [])))


(defrecord ConsoleLogger []
  ILog
  (log [self message]
    (println message)))

(defn >ConsoleLogger []
  (->ConsoleLogger))

(defrecord MailLogger [mailer from to subject]
  ILog
  (log [self message]
    (send-mailmessage mailer {:from from :to to :subject subject :text message})))

(defn >MailLogger [mailer from to subject]
  (->MailLogger mailer from to subject))

(defrecord CompositeLogger [loggers]
  ILog
  (log [self message]
    (doseq [logger loggers]
      (log logger message))))

(defn >CompositeLogger [& loggers]
  (->CompositeLogger loggers))

(defn >MailConsoleLogger [mailer from to subject]
  (->CompositeLogger
    (->ConsoleLogger) 
    (->MailLogger mailer from to subject)))
