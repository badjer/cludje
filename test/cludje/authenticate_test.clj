(ns cludje.authenticate-test
  (:require [cemerick.friend :as friend]
            [cemerick.friend.credentials :as creds])
  (:use midje.sweet
        cludje.test
        cludje.authenticate
        cludje.system
        cludje.datastore))

(def mockuser {:username "a@b.cd" :password "123"})
(def db-mockuser (assoc mockuser :hashed-pwd "123"))
(defn >datastore [] (>TestDatastore {:user [db-mockuser]}))
(defn >login-system [] {:data-store (>datastore)})
(defn >login-context [] {:system (>login-system) :input mockuser})

(defn test-authenticator [auth context]
  (fact "is an IAuthenticator"
    (satisfies? IAuthenticator auth) => true)
  (fact "current-user - not logged in"
    (current-user auth context) => nil)
  (let [loggedin-context (log-in auth context)]
    (fact "log-in"
      loggedin-context => ok?)
    (fact "current-user - logged in"
      (current-user auth loggedin-context) => mockuser)
    (let [loggedout-context (log-out auth loggedin-context)]
      (fact "log-out"
        loggedout-context => ok?)
      (fact "current-user - logged out"
        (current-user auth loggedout-context) => nil))))


(fact "TestAuthenticator"
  (test-authenticator 
    (>TestAuthenticator) 
    (>login-context)))

(fact "TokenAuthenticator"
  (test-authenticator
    (>TokenAuthenticator :test-app)
    (>login-context)))

(fact "TokenLogin sets auth token"
  (let [auth (>TokenAuthenticator :test-app)]
    (log-in auth (>login-context)) => (contains {:session {:test-app "a@b.cd"}})))

(future-facts "TokenLogin - repeated calls to current-user don't hit the db repeatedly")
; Cache the current user in meta-data on the input, and then just re-read it
; in subsequent calls to current-user?

(future-facts "TokenLogin does encryption and has security")

(def hashed-mockuser (assoc mockuser :hashed-pwd (creds/hash-bcrypt "123")))

(def friend-request {:session 
                     {::friend/identity 
                      {:current :a 
                       :authentications {:a mockuser}}}})
(def friend-context {:raw-input friend-request})
(def empty-friend-context {:raw-input {:session {}} :input mockuser})

(fact "friend works as expected"
  (friend/current-authentication friend-request) => mockuser)

(def get-user (constantly hashed-mockuser))

(fact "FriendAuthenticator"
  (test-authenticator (>FriendAuthenticator get-user)
                      empty-friend-context)
  (let [auth (>FriendAuthenticator get-user)]
    (fact "current-user with existing friend session"
      (current-user auth friend-context) => mockuser)))
