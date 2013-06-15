(ns cludje.core-test
  (:use midje.sweet
        cludje.test
        cludje.types
        cludje.core))

(defmodel User {:name Str :email Email :pwd Password})

(fact "defmodel"
  (fact "constructor"
    (User) =not=> nil
    (User) => (has-keys :name :email :pwd)
    (User {:name "abc"}) => (has-keys :name :email :pwd)
    (User {:name "abc"}) => (contains {:name "abc"})
    (User :name "abc") => (contains {:name "abc"})
    (User :name "abc" :email "d@e.fg") => 
      (contains {:name "abc" :email "d@e.fg"}))
  (fact "get-problems? checks field existence"
    (user-problems? {}) =not=> empty?
    (user-problems? {}) => (has-keys :name :email :pwd)
    (user-problems? {:name "a"}) => (just-keys :email :pwd))
  )
  
