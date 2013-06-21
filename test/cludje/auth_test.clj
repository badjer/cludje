(ns cludje.auth-test
  (:use midje.sweet
        cludje.core
        cludje.auth))

(fact "mockauth"
  (let [auth (->MockAuth (atom false))]
    (current-user- auth) => nil
    (login- auth mockuser) => anything
    (current-user- auth) => mockuser
    (logout- auth) => anything
    (current-user- auth) => nil
    (authorize auth mockuser {}) => truthy
    (authorize auth nil {}) => falsey))
