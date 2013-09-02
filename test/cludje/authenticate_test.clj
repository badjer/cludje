(ns cludje.authenticate-test
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
