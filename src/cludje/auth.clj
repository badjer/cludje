(ns cludje.auth
  (:use cludje.core))

(def mockuser {:username "a@b.cd" :pwd "123"})

(defrecord MockAuth [logged-in? authfn]
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
  (authorize- [self action model user input] 
    (@authfn action model user input)))

(defn- default-auth-fn [action model user input]
  (= mockuser (select-keys user (keys mockuser))))

(defn make-MockAuth [logged-in? & authfns]
  (let [authfn (if (seq authfns)
                 (fn [action model user input] 
                   (some #{true} 
                         (map #(% action model user input) authfns))) 
                 default-auth-fn)]
    (->MockAuth (atom logged-in?) (atom authfn))))
