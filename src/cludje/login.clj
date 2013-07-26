(ns cludje.login
  (:use cludje.core))

(def mockuser {:username "a@b.cd" :pwd "123" :hashed-pwd "123"})

; TODO: Replace this with TestLogin everywhere and delete this?
(defrecord MockLogin [logged-in?]
  ILogin
  (current-user- [self input] (when @logged-in? mockuser))
  (login- [self input] 
    (when (= mockuser (select-keys input (keys mockuser))) 
      (reset! logged-in? true) 
      true))
  (logout- [self input] 
    (reset! logged-in? false)
    true)
  (encrypt- [self txt] txt)
  (check-hash- [self txt cypher] (= (encrypt self txt) cypher)))

(defn make-MockLogin [{:keys [logged-in?]}]
  (->MockLogin (atom logged-in?)))


(defrecord TestLogin [current]
  ILogin
  (current-user- [self input] @current)
  (login- [self input] (reset! current (select-keys input [:username :pwd])))
  (logout- [self input] (reset! current nil))
  (encrypt- [self txt] txt)
  (check-hash- [self txt cypher] (= (encrypt self txt) cypher)))

(defn make-TestLogin
  ([] (->TestLogin (atom nil)))
  ([user] (->TestLogin (atom user))))

(defn- token->user [token db user-table]
  (first (query- db user-table {:username token})))

(defn- validate-user [{:keys [username pwd]} db user-table check-hash-fn]
  ; TODO: Assumes that the username is the PK
  (when-let [user (first (query- db user-table {:username username}))]
    (when (check-hash-fn pwd (:hashed-pwd user))
      user)))

(defn- make-token [user] (:username user))

(defn- expire-token [input] (assoc input :_p_cludjeauthtoken ""))


(defrecord TokenLogin [secret db user-table]
  ILogin
  (current-user- [self input]
    (when-let [token (:_p_cludjeauthtoken input)]
      (token->user token db user-table)))
  (login- [self input]
    (if-let [user (validate-user 
                    input db user-table (partial check-hash- self))]
      (assoc input :_p_cludjeauthtoken (make-token user))
      (throw-problems {:username "Invalid username/password"
                       :pwd "Invalid username/password"})))
  (logout- [self input] (expire-token input))
  (encrypt- [self txt] txt)
  (check-hash- [self txt cypher] (= txt cypher)))

