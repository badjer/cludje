(ns cludje.authenticate
  (:use cludje.system))

(defrecord TestAuthenticator [cur-user]
  IAuthenticator
  (current-user [self context] @cur-user)
  (log-in [self context user] (reset! cur-user user))
  (log-out [self context] (reset! cur-user nil)))

(defn >TestAuthenticator [cur-user]
  (->TestAuthenticator (atom cur-user)))
