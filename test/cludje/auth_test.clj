(ns cludje.auth-test
  (:use midje.sweet
        cludje.core
        cludje.auth))

(fact "mockauth"
  (let [auth (make-MockAuth false)]
    (current-user- auth) => nil
    (login- auth mockuser) => anything
    (current-user- auth) => mockuser
    (logout- auth) => anything
    (current-user- auth) => nil
    (authorize auth :action :model mockuser {}) => truthy
    (authorize auth :action :model nil {}) => falsey))
