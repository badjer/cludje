(ns cludje.authenticate
  (:require [cemerick.friend :as friend]
            [cemerick.friend.credentials :as creds]
            [clojure.string :as st])
  (:use cludje.system
        cludje.errors
        cludje.mold
        cludje.types
        cludje.util))

(def LoginUser (>Mold {:username Email :password Password} {}))

(defrecord TestAuthenticator [cur-user]
  IAuthenticator
  (current-user [self request] @cur-user)
  (log-in [self request] (reset! cur-user (parse LoginUser (? request :input))))
  (log-out [self request] (reset! cur-user nil))
  (encrypt [self txt] (st/reverse txt))
  (check-hash [self txt cypher] (= (encrypt self txt) cypher)))

(defn >TestAuthenticator 
  ([] (>TestAuthenticator nil))
  ([cur-user]
    (->TestAuthenticator (atom cur-user))))


(defn- token->user [token datastore user-table]
  (-> (query datastore user-table {:username token})
      (first)
      (dissoc :hashed-pwd)))

(defn- validate-user [authenticator input datastore user-table]
  (let [username (? input :username)
        password (? input :password)]
    (when-let [user (first (query datastore user-table {:username username}))]
      (when (check-hash authenticator password (? user :hashed-pwd))
        user))))

(defn- make-token [user] (:username user))

(defn- expire-token [request app-name] 
  (assoc-in request [:session app-name] ""))


(defrecord TokenAuthenticator [app-name user-table]
  IAuthenticator
  (current-user [self request]
    (when-let [token (?? (?! request :session) app-name)]
      (let [datastore (?! request [:system :data-store])]
        (token->user token datastore user-table))))
  (log-in [self request]
    (let [datastore (?! request [:system :data-store])
          input (?! request :input)
          user (parse LoginUser input)]
      (if-let [val-user (validate-user self input datastore user-table)]
        (assoc-in request [:session app-name] (make-token val-user))
        (throw-problems {:username "Invalid username/password" 
                         :password "Invalid username/password"}))))
  (log-out [self request] 
    (expire-token request app-name))
  (encrypt [self txt] (st/reverse txt))
  (check-hash [self txt cypher] (= (encrypt self txt) cypher)))

(defn >TokenAuthenticator
  ([app-name] (>TokenAuthenticator app-name :user))
  ([app-name user-table] (->TokenAuthenticator app-name user-table)))


(defn >friend-map [user] 
  (let [clean-user (dissoc user :hashed-pwd)]
    {:current :a 
     :authentications {:a clean-user}}))

(defrecord FriendAuthenticator [get-user-fn]
  IAuthenticator
  (current-user [self request]
    (friend/current-authentication request))
  (log-in [self request]
    (let [{:keys [username password]} (make LoginUser (?! request :params))]
      (when-let [got-user (get-user-fn username)]
        (if (check-hash self password (:hashed-pwd got-user))
          (assoc-in request [:session ::friend/identity] (>friend-map got-user))))))
  (log-out [self request]
    (update-in request [:session] dissoc ::friend/identity))
  (encrypt [self txt]
    (creds/hash-bcrypt txt))
  (check-hash [self txt cypher]
    (when (and txt cypher)
      (creds/bcrypt-verify txt cypher))))



(defn >FriendAuthenticator [get-user-fn]
  (->FriendAuthenticator get-user-fn))
