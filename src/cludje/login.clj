(ns cludje.login
  (:use cludje.core))

(def mockuser {:username "a@b.cd" :pwd "123"})

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

(defrecord TokenLogin []
  ILogin
  (current-user- [self input])
  (login- [self input])
  (logout- [self input])
  (encrypt- [self txt])
  (check-hash- [self txt cypher]))



