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
      (current-user- lgin li-input) => (contains (dissoc user :hashed-pwd))
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
  (let [lgin (->TokenLogin "abc123" (mockuser-db) :user)]
    (test-login lgin mockuser)))

(let [lgin (->TokenLogin "abc123" (mockuser-db) :user)]
  (fact "TokenLogin sets auth token"
    (login- lgin mockuser) => (contains {:_authtoken anything})))

(future-facts "TokenLogin - repeated calls to current-user- don't hit the db repeatedly")
; Cache the current user in meta-data on the input, and then just re-read it
; in subsequent calls to current-user-?

(future-facts "TokenLogin does encryption and has security")
