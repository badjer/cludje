(ns cludje.authenticate
  (:require [cemerick.friend :as friend]
            [cemerick.friend.credentials :as creds])
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
  (log-out [self request] (reset! cur-user nil)))

(defn >TestAuthenticator 
  ([] (>TestAuthenticator nil))
  ([cur-user]
    (->TestAuthenticator (atom cur-user))))


(defn- token->user [token datastore user-table]
  (-> (query datastore user-table {:username token})
      (first)
      (dissoc :hashed-pwd)))

(defn- validate-user [input datastore user-table hash-fn]
  (let [username (? input :username)
        password (? input :password)]
    (when-let [user (first (query datastore user-table {:username username}))]
      (when (= (? user :hashed-pwd)
               (hash-fn password))
        user))))

(defn- make-token [user] (:username user))

(defn- expire-token [request app-name] 
  (assoc-in request [:session app-name] ""))

(defn- hash-password [text]
  ; TODO: Actual hashing
  text)


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
      (if-let [val-user (validate-user input datastore user-table hash-password)]
        (assoc-in request [:session app-name] (make-token val-user))
        (throw-problems {:username "Invalid username/password" 
                         :password "Invalid username/password"}))))
  (log-out [self request] 
    (expire-token request app-name)))

(defn >TokenAuthenticator
  ([app-name] (>TokenAuthenticator app-name :user))
  ([app-name user-table] (->TokenAuthenticator app-name user-table)))


(defn check-pwd [input-pwd hashed-pwd]
  (creds/bcrypt-verify input-pwd hashed-pwd))

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
        (if (check-pwd password (:hashed-pwd got-user))
          (assoc-in request [:session ::friend/identity] (>friend-map got-user))))))
  (log-out [self request]
    (update-in request [:session] dissoc ::friend/identity))) 


(defn >FriendAuthenticator [get-user-fn]
  (->FriendAuthenticator get-user-fn))
