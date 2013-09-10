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
(defn >login-request [] {:system (>login-system) :input mockuser :session {}})

(defn test-authenticator [auth request]
  (fact "is an IAuthenticator"
    (satisfies? IAuthenticator auth) => true)
  (fact "current-user - not logged in"
    (current-user auth request) => nil)
  (let [loggedin-request (log-in auth request)]
    (fact "log-in"
      loggedin-request => ok?)
    (fact "current-user - logged in"
      (current-user auth loggedin-request) => mockuser)
    (let [loggedout-request (log-out auth loggedin-request)]
      (fact "log-out"
        loggedout-request => ok?)
      (fact "current-user - logged out"
        (current-user auth loggedout-request) => nil))))

(fact "TestAuthenticator"
  (test-authenticator 
    (>TestAuthenticator) 
    (>login-request)))

(fact "TokenAuthenticator"
  (test-authenticator
    (>TokenAuthenticator :test-app)
    (>login-request)))


(fact "TokenLogin sets auth token"
  (let [auth (>TokenAuthenticator :test-app)]
    (log-in auth (>login-request)) => (contains {:session {:test-app "a@b.cd"}})))

(future-facts "TokenLogin - repeated calls to current-user don't hit the db repeatedly")
; Cache the current user in meta-data on the input, and then just re-read it
; in subsequent calls to current-user?

(future-facts "TokenLogin does encryption and has security")

(def hashed-mockuser (assoc mockuser :hashed-pwd (creds/hash-bcrypt "123")))

(def friend-request {:session 
                     {::friend/identity 
                      {:current :a 
                       :authentications {:a mockuser}}}})
(def empty-friend-request {:params mockuser :session {}})

(fact "friend works as expected"
  (friend/current-authentication friend-request) => mockuser)

(def get-user (constantly hashed-mockuser))

(fact "FriendAuthenticator"
  (test-authenticator (>FriendAuthenticator get-user)
                      empty-friend-request)
  (let [auth (>FriendAuthenticator get-user)]
    (fact "current-user with existing friend session"
      (current-user auth friend-request) => mockuser)))
