(ns cludje.authorize
  (:use cludje.system))

(defrecord TestAuthorizer [allow?]
  IAuthorizer
  (allowed? [self system action-sym user input]
    @allow?))

(defn >TestAuthorizer [allow?]
  (->TestAuthorizer (atom allow?)))
