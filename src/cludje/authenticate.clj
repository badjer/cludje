(ns cludje.authenticate
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

(defn- validate-user [input datastore user-table check-hash-fn]
  (let [username (? input :username)
        password (? input :password)]
    (when-let [user (first (query datastore user-table {:username username}))]
      (when (check-hash-fn password (? user :hashed-pwd))
        user))))

(defn- make-token [user] (:username user))

(defn- expire-token [context app-name] 
  (assoc-in context [:session app-name] ""))

(defn- check-hash [text hashed]
  ; TODO: Actual hashing
  (= text hashed))

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
      (if-let [val-user (validate-user input datastore user-table check-hash)]
        (assoc-in context [:session app-name] (make-token val-user))
        (throw-problems {:username "Invalid username/password" 
                         :password "Invalid username/password"}))))
  (log-out [self context] 
    (expire-token context app-name)))

(defn >TokenAuthenticator
  ([app-name] (>TokenAuthenticator app-name :user))
  ([app-name user-table] (->TokenAuthenticator app-name user-table)))
