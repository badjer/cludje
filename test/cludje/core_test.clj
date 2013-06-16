(ns cludje.core-test
  (:use midje.sweet
        cludje.test
        cludje.types
        cludje.core))

(defmodel User {:name Str :email Email :pwd Password})
(defmodel Cog {:price Money :amt Int})

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
  (fact "parse returns a map with all keys"
    (parse-user {}) => (has-keys :name :email :pwd)
    (parse-user {:name "a"}) => (has-keys :name :email :pwd))
  (fact "parse converts field values"
    (parse-cog {:amt "12"}) => (contains {:amt 12})
    (parse-cog {:price "$12.34"}) => (contains {:price 1234}))
  )
