(ns cludje.datastore-test
  (:use midje.sweet
        cludje.system
        cludje.mold
        cludje.model
        cludje.types
        cludje.test
        cludje.datastore))

(def tbl "table")
(def kw-tbl (keyword tbl))
(def row {:a 1})
(def row2 {:a 2})
(def rowb {:a 1 :b 1})
(def row-with-kw {:a :kw})

(defn test-db [constructor]
  (let [db (constructor)
        id (write db tbl nil row)]
    (fact "write" 
      id =not=> nil?
      (fact "rejects keyword data"
        (write db tbl nil row-with-kw) => (throws)))
    (fact "fetch"
      (fetch db tbl id) => (contains row)
      (fetch db tbl -1) => nil
      (fetch db tbl nil) => nil
      (fetch db "asdf" -1) => nil
      (fact "rejects keyword data"
        (fetch db tbl row-with-kw) => (throws)))
    (fact "fetch with keyword table"
      (fetch db kw-tbl id) => (contains row)
      (fetch db kw-tbl -1) => nil)
    (fact "query"
      (count (query db tbl {:a 1})) => 1
      (first (query db tbl {:a 1})) => (contains row)
      (query db tbl {:a 2}) => nil
      (count (query db tbl {})) => 1
      (first (query db tbl {:a 1})) => (contains row)
      (count (query db tbl nil)) => 1
      (first (query db tbl nil)) => (contains row)
      (fact "rejects keyword data"
        (query db tbl row-with-kw) => (throws)))
    (fact "query with keyword table"
      (count (query db kw-tbl {:a 1})) => 1
      (first (query db kw-tbl {:a 1})) => (contains row))
    (fact "update"
      (write db tbl id row2) => id
      (fetch db tbl id) => (contains row2))
    (fact "delete"
      (count (query db tbl nil)) => 1
      ; Delete nil shouldn't do anything
      (delete db tbl nil) => anything
      (count (query db tbl nil)) => 1
      (delete db tbl id) => anything
      (count (query db tbl nil)) => 0
      (fact "rejects keyword data"
        (delete db tbl row-with-kw) => (throws)))
    (fact "query with empty table"
      (query db tbl nil) => nil)
    (let [id (write db tbl nil row)
          idb (write db tbl nil rowb)]
      (fact "query with multiple rows"
        (count (query db tbl {:a 1})) => 2
        (count (query db tbl {:b 1})) => 1
        (count (query db tbl nil)) => 2)
      (fact "update with multiple rows"
        (write db tbl id row2) => id
        (fetch db tbl id) => (contains row2)
        (fetch db tbl idb) => (contains rowb))
      (fact "delete with multiple rows"
        (count (query db tbl nil)) => 2
        (delete db tbl id) => anything
        (count (query db tbl nil)) => 1
        (first (query db tbl nil)) => (contains rowb)))))

(def Cog (>Model "Cog" {:price Money :amt Int} {}))
(def cog {:price 123 :amt 1})
(def cog2 {:price 123 :amt 2})
(def cogb {:price 234 :amt 1})

(defn test-higher-level [constructor]
  (fact "save"
    (let [ds (constructor)]
      (fact "save exceptions"
        (save ds Cog {}) => (throws)
        (save ds Cog {:price 123}) => (throws)
        (save ds Cog {:price 123 :amt 1}) => anything
        (save ds Cog {:price "abc" :amt 1}) => (throws-problems)
        (save ds Cog {:price 123 :amt "a"}) => (throws))
      (fact "save with extra fields is fine"
        (save ds Cog {:price 123 :amt 1 :x 1}) => anything)
      (fact "save returns something key-like"
        (save ds Cog cog) =not=> empty?)))

  (fact "save will set the key field"
    (let [ds (constructor)
          kee (save ds Cog cog)]
      (fetch ds Cog (:_id kee)) => (contains kee)))

  (fact "save knows when to insert"
    (let [ds (constructor)
          id (write ds Cog nil cog)]
      (save ds Cog cog) =not=> id
      (count (query ds Cog nil)) => 2))

  (fact "save knows when to update"
    (let [ds (constructor)
          kee (save ds Cog cog)]
      (count (query ds Cog nil)) => 1
      (save ds Cog (merge cog kee)) => anything
      (count (query ds Cog nil)) => 1))

  (fact "insert"
    (let [ds (constructor)]
      (fact "insert exceptions"
        (insert ds Cog {}) => (throws)
        (insert ds Cog {:price 123}) => (throws)
        (insert ds Cog {:price 123 :amt 1}) => anything
        (insert ds Cog {:price "abc" :amt 1}) => (throws)
        (insert ds Cog {:price 123 :amt "a"}) => (throws))
      (fact "insert with extra fields is fine"
        (insert ds Cog {:price 123 :amt 1 :x 1}) => anything)
      (fact "insert returns something key-like"
        (insert ds Cog cog) =not=> empty?)))

  (fact "insert will set the key field"
    (let [ds (constructor)
          kee (insert ds Cog cog)]
      (fetch ds Cog (:_id kee)) => (contains kee)))

  (fact "insert knows when to insert"
    (let [ds (constructor)
          id (write ds Cog nil cog)]
      (insert ds Cog cog) =not=> id
      (count (query ds Cog nil)) => 2))

  (fact "insert knows when to update"
    (let [ds (constructor)
          id (insert ds Cog cog)]
      (count (query ds Cog nil)) => 1
      (insert ds Cog cog) => anything
      (count (query ds Cog nil)) => 2))

  (facts "db operations work with keyword table names"
    (let [ds (constructor)
          id (write ds :cog nil cog)]
      (count (query ds :cog nil)) => 1
      (fetch ds :cog id) => (contains cog)
      (delete ds :cog id) => anything
      (query ds :cog nil) => nil))

  (facts "save and insert return a map"
    (let [ds (constructor)]
      (insert ds Cog cog) => map?
      (save ds Cog cog) => map?))

  (facts "db-writing ops return maps"
    (let [ds (constructor)]
      (save ds Cog cog) => map?
      (save ds Cog cog) => (has-keys :_id)
      (:_id (save ds Cog cog)) => string?
      (insert ds Cog cog) => map?
      (insert ds Cog cog) => (has-keys :_id)
      (:_id (insert ds Cog cog)) => string?)))



(facts "TestDatastore"
  (test-db >TestDatastore)
  (test-higher-level >TestDatastore))

(def test-mongo-db "cludje-datastore-test")
(def test-mongo-uri (str "mongodb://127.0.0.1/" test-mongo-db))

(defn >mongo-test []
  (drop-mongo! test-mongo-uri test-mongo-db)
  (>MongoDatastore test-mongo-uri))

(facts "MongoDatastore"
  (test-db >mongo-test)
  (test-higher-level >mongo-test))


(future-facts "Implement soft deletes")
