(ns cludje.login-test
  (:use midje.sweet
        cludje.core
        cludje.database
        cludje.login))

(defn test-login [lgin user]
  (fact "works"
    (current-user- lgin user) => nil
    (let [li-input (login- lgin user)] 
      li-input => anything
      (current-user- lgin li-input) => (contains user)
      (let [lo-input (logout- lgin li-input)] 
        lo-input => anything 
        (current-user- lgin lo-input) => nil))))

(fact "MockLogin"
  (let [lgin (make-MockLogin false)]
    (test-login lgin mockuser)))

(fact "TestLogin"
  (let [lgin (make-TestLogin)]
    (test-login lgin mockuser)))

(defn- mockuser-db []
  (->MemDb (atom {:user [(assoc mockuser :hashed-pwd "123")]})))

(fact "TokenLogin"
  (let [lgin (->TokenLogin (atom "abc123") (mockuser-db) :user)]
    (test-login lgin mockuser)))

(let [lgin (->TokenLogin (atom "abc123") (mockuser-db) :user)]
  (fact "TokenLogin sets auth token"
    (login- lgin mockuser) => (contains {:_authtoken anything})))

(future-facts "TokenLogin does encryption and has security")
