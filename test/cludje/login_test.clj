(ns cludje.login-test
  (:use midje.sweet
        cludje.core
        cludje.login))

(defn test-login [lgin user]
  (fact "works"
    (current-user- lgin user) => nil
    (login- lgin user) => anything
    (current-user- lgin user) => user
    (logout- lgin user) => anything
    (current-user- lgin user) => nil))


(fact "MockLogin"
  (let [lgin (make-MockLogin false)]
    (test-login lgin mockuser)))

(fact "TestLogin"
  (let [user {:username "a@b.cd" :pwd "a"}
        lgin (make-TestLogin)]
    (test-login lgin user)))

(fact "TokenLogin"
  (let [user {:username "a@b.cd" :pwd "a"}
        lgin (->TokenLogin)]
    1 => 2
))
