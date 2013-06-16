(ns cludje.core-test
  (:use midje.sweet
        cludje.test
        cludje.types
        cludje.core))

(defmodel User {:name Str :email Email :pwd Password})
(defmodel Cog {:price Money :amt Int})

(fact "defmodel"
  (fact "constructor"
    User =not=> nil
    User => (has-keys :name :email :pwd)
    (make User {:name "abc"}) => (has-keys :name :email :pwd)
    (make User {:name "abc"}) => (contains {:name "abc"})
    (make User :name "abc") => (contains {:name "abc"})
    (make User :name "abc" :email "d@e.fg") => 
      (contains {:name "abc" :email "d@e.fg"}))
  (fact "problems? checks field existence"
    (problems? User {}) =not=> empty?
    (problems? User {}) => (has-keys :name :email :pwd)
    (problems? User {:name "a"}) => (just-keys :email :pwd))
  (fact "parse returns a map with all keys"
    (make User {}) => (has-keys :name :email :pwd)
    (make User {:name "a"}) => (has-keys :name :email :pwd))
  (fact "parse converts field values"
    (make Cog {:amt "12"}) => (contains {:amt 12})
    (make Cog {:price "$12.34"}) => (contains {:price 1234}))
  (fact "problems? checks type"
    (problems? Cog {:amt "asdf"}) => (has-keys :amt))
  )

