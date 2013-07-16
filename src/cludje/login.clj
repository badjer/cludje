(ns cludje.login
  (:use cludje.core))

(def mockuser {:username "a@b.cd" :pwd "123"})

; TODO: Replace this with TestLogin everywhere and delete this?
(defrecord MockLogin [logged-in?]
  ILogin
  (current-user- [self] (when @logged-in? mockuser))
  (login- [self user] 
    (when (= mockuser (select-keys user (keys mockuser))) 
      (reset! logged-in? true) 
      true))
  (logout- [self] 
    (reset! logged-in? false)
    true)
  (encrypt- [self txt] txt)
  (check-hash- [self txt cypher] (= (encrypt self txt) cypher)))

(defn make-MockLogin [{:keys [logged-in?]}]
  (->MockLogin (atom logged-in?)))


(defrecord TestLogin [u]
  ILogin
  (current-user- [self] @u)
  (login- [self user] (reset! u user))
  (logout- [self] (reset! u nil))
  (encrypt- [self txt] txt)
  (check-hash- [self txt cypher] (= (encrypt self txt) cypher)))

(defn make-TestLogin
  ([] (->TestLogin (atom nil)))
  ([user] (->TestLogin (atom user))))

