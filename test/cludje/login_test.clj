(ns cludje.login-test
  (:use midje.sweet
        cludje.core
        cludje.login))

(defn test-login [lgin user]
  (fact "works"
    (current-user- lgin) => nil
    (login- lgin user) => anything
    (current-user- lgin) => user
    (logout- lgin) => anything
    (current-user- lgin) => nil))

(fact "MockLogin"
  (let [lgin (make-MockLogin false)]
    (test-login lgin mockuser)))

(fact "TestLogin"
  (let [user {:username "a@b.cd" :pwd "a"}
        lgin (make-TestLogin)]
    (test-login lgin user)))

(fact "FriendLogin"
  (let [user {:username "a@b.cd" :pwd "a"}
        lgin (->FriendLogin)]
    1 => 2
))
