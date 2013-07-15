(ns cludje.login-test
  (:use midje.sweet
        cludje.core
        cludje.login))

(fact "MockLogin"
  (let [login (make-MockLogin false)]
    (current-user- login) => nil
    (login- login mockuser) => anything
    (current-user- login) => mockuser
    (logout- login) => anything
    (current-user- login) => nil))

(fact "TestLogin"
  (let [user {:username "a@b.cd" :pwd "a"}
        lgin (make-TestLogin)]
    (current-user- lgin) => nil
    (login- lgin user) => anything
    (current-user- lgin) => user
    (logout- lgin) => anything
    (current-user- lgin) => nil))

