(ns cludje.email
  (:use cludje.system)
  (:require [clj-http.client :as http]
            [cludje.validate :as vali]))

(defrecord TestEmailer [messages]
  IEmailer
  (send-mailmessage [self message]
    (swap! messages conj message)))

(defn >TestEmailer []
  (->TestEmailer (atom [])))


(defrecord LogEmailer [logger]
  IEmailer
  (send-mailmessage [self message] (log logger message)))


(defn mailgun-api-key []
  (get (System/getenv) "MAILGUN_API_KEY" ""))

(defn mailgun-appname []
  (let [smtp-login (get (System/getenv) "MAILGUN_SMTP_LOGIN" "")]
    (last (re-find #"@(.*)$" smtp-login))))

(defn mailgun-url 
  ([] (mailgun-url (mailgun-api-key) (mailgun-appname)))
  ([api-key appname]
    (str "https://api:" api-key "@api.mailgun.net/v2/" appname)))

(defn message-problems? [message]
  "Makes sure that a message is valid; that it can be sent"
  (merge
    (vali/needs message :to :from :subject :text)))


(defrecord MailgunMailer [url]
  IEmailer
  (send-mailmessage [self message]
    {:pre [#(not (message-problems? message))]}
    (http/post (str url "/messages") {:form-params message})))
      
(defn >MailgunMailer 
  ([] (>MailgunMailer (mailgun-url)))
  ([url] (->MailgunMailer url)))
