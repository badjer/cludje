(ns cludje.auth
  (:use cludje.core))

(def mockuser {:username "a@b.cd" :pwd "123"})
(def mockroles [:guest])

(defrecord MockAuth [logged-in?]
  IAuth
  (current-user- [self] (when @logged-in? mockuser))
  (login- [self user] 
    (when (= mockuser (select-keys user (keys mockuser))) 
      (reset! logged-in? true) 
      true))
  (logout- [self] 
    (reset! logged-in? false)
    true)
  (encrypt- [self txt] txt)
  (check-hash- [self txt cypher] (= (encrypt self txt) cypher))
  (in-role?- [self user role] 
    (and (= mockuser (select-keys user (keys mockuser)))
         (some #{role} mockroles))))

