(ns cludje.core-test
  (:use midje.sweet
        cludje.test
        cludje.types
        cludje.database
        cludje.core))

(defmodel User {:name Str :email Email :pwd Password})
(defmodel Cog {:price Money :amt Int})
(defmodel Person {:name Str :age Int} :require [:name])

(fact "defmodel"
  (fact "metadata"
    (meta User) => (has-keys :fields :require :table)
    (:fields (meta User)) => (has-keys :name :email :pwd)
    (:email (:fields (meta User))) => Email
    (:require (meta User)) => [:name :email :pwd]
    (:table (meta User)) => "user")
  (fact "make"
    User =not=> nil
    User => (has-keys :name :email :pwd)
    (make User {:name "abc"}) => (has-keys :name :email :pwd)
    (make User {:name "abc"}) => (contains {:name "abc"})
    (make User :name "abc") => (contains {:name "abc"})
    (make User :name "abc" :email "d@e.fg") => 
      (contains {:name "abc" :email "d@e.fg"}))
  (fact "make adds key"
    (make User {}) => (has-keys :user_id))
  (fact "problems? checks field existence"
    (problems? User {}) =not=> empty?
    (problems? User {}) => (has-keys :name :email :pwd)
    (problems? User {:name "a"}) => (just-keys :email :pwd))
  (fact "problems? returns empty if no problems"
    (problems? Person {:name "abc" :age 2}) => nil)
  (fact "make returns a map with all keys"
    (make User {}) => (has-keys :name :email :pwd)
    (make User {:name "a"}) => (has-keys :name :email :pwd))
  (fact "make removes any extra keys"
    (make User {:foosums "a"}) => (just-keys :name :email :pwd :user_id))
  (fact "parse converts field values"
    (make Cog {:amt "12"}) => (contains {:amt 12})
    (make Cog {:price "$12.34"}) => (contains {:price 1234}))
  (fact "problems? checks type"
    (problems? Cog {:amt "asdf"}) => (has-keys :amt))
  (fact "problems? only needs required fields"
    (problems? Person {}) => (just-keys :name)))

(def cog {:price 123 :amt 1})
(def cog2 {:price 123 :amt 2})
(def cogb {:price 234 :amt 1})

(fact "write"
  (let [dba (atom {})
        db (->MemDb dba)]
    (write db Cog nil cog) => anything
    @dba => (just-keys :cog)
    (count (get @dba :cog)) => 1))

(fact "write will update"
  (let [db (->MemDb (atom {}))
        id (write db Cog nil cog)]
    (write db Cog id cog2) => id
    (count (query db Cog nil)) => 1))


(fact "fetch"
  (let [db (->MemDb (atom {}))
        id (write db Cog nil cog)]
    (fetch db Cog id) => (contains cog)
    (fetch db Cog nil) => nil
    (fetch db nil nil) => nil
    (fetch db nil id) => nil))

(fact "fetch with multiple rows"
  (let [db (->MemDb (atom {}))
        id (write db Cog nil cog)
        idb (write db Cog nil cogb)]
    (fetch db Cog id) => (contains cog)
    (fetch db Cog idb) => (contains cogb)))


(fact "save"
  (let [db (->MemDb (atom {}))]
    (fact "save exceptions"
      (save db Cog {}) => (throws Exception)
      (save db Cog {:price 123}) => (throws Exception)
      (save db Cog {:price 123 :amt 1}) => anything
      (save db Cog {:price "abc" :amt 1}) => (throws Exception)
      (save db Cog {:price 123 :amt "a"}) => (throws Exception))
    (fact "save with extra fields is fine"
      (save db Cog {:price 123 :amt 1 :x 1}) => anything)
    (fact "save returns something key-like"
      (save db Cog cog) =not=> empty?)))

(fact "save knows when to insert"
  (let [db (->MemDb (atom {}))
        id (write db Cog nil cog)]
    (save db Cog cog) =not=> id
    (count (query db Cog nil)) => 2))

(fact "get-key"
  (get-key Cog {:cog_id 1}) => 1
  (get-key Cog {}) => nil)

(fact "save knows when to update"
  (let [db (->MemDb (atom {}))
        id (write db Cog nil cog)
        with-id (assoc cog :cog_id id)]
    (save db Cog with-id) => id
    (count (query db Cog nil)) => 1))


