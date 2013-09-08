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
  (current-user [self context] @cur-user)
  (log-in [self context] (reset! cur-user (parse LoginUser (? context :input))))
  (log-out [self context] (reset! cur-user nil)))

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

(defn- expire-token [context app-name] 
  (assoc-in context [:session app-name] ""))

(defn- hash-password [text]
  ; TODO: Actual hashing
  text)


(defrecord TokenAuthenticator [app-name user-table]
  IAuthenticator
  (current-user [self context]
    (when-let [token (?? context [:session app-name])]
      (let [datastore (? context [:system :data-store])]
        (token->user token datastore user-table))))
  (log-in [self context]
    (let [datastore (? context [:system :data-store])
          input (? context :input)
          user (parse LoginUser input)]
      (if-let [val-user (validate-user input datastore user-table hash-password)]
        (assoc-in context [:session app-name] (make-token val-user))
        (throw-problems {:username "Invalid username/password" 
                         :password "Invalid username/password"}))))
  (log-out [self context] 
    (expire-token context app-name)))

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
  (current-user [self context]
    (let [request (?! context :raw-input)]
      (friend/current-authentication request)))
  (log-in [self context]
    (let [{:keys [username password]} (make LoginUser (?! context :input))]
      (when-let [got-user (get-user-fn username)]
        (if (check-pwd password (:hashed-pwd got-user))
          (assoc-in context [:raw-input :session ::friend/identity] (>friend-map got-user))))))
  (log-out [self context]
    (update-in context [:raw-input :session] dissoc ::friend/identity))) 


(defn >FriendAuthenticator [get-user-fn]
  (->FriendAuthenticator get-user-fn))
