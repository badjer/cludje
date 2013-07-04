(ns cludje.login-test
  (:use midje.sweet
        cludje.core
        cludje.login))

(fact "mocklogin"
  (let [login (make-MockLogin false)]
    (current-user- login) => nil
    (login- login mockuser) => anything
    (current-user- login) => mockuser
    (logout- login) => anything
    (current-user- login) => nil))
