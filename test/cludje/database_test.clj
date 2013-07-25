(ns cludje.database-test
  (:use midje.sweet
        cludje.core
        cludje.database))

(def tbl "table")
(def kw-tbl (keyword tbl))
(def row {:a 1})
(def row2 {:a 2})
(def rowb {:a 1 :b 1})
(def row-with-kw {:a :kw})

(defn test-db [db]
  (let [id (write- db tbl nil row)]
    (fact "write-" 
      id =not=> nil?
      (fact "rejects keyword data"
        (write- db tbl nil row-with-kw) => (throws)))
    (fact "fetch-"
      (fetch- db tbl id) => (contains row)
      (fetch- db tbl -1) => nil
      (fetch- db tbl nil) => nil
      (fetch- db "asdf" -1) => nil
      (fetch- db nil nil) => nil
      (fact "rejects keyword data"
        (fetch- db tbl row-with-kw) => (throws)))
    (fact "fetch- with keyword table"
      (fetch- db kw-tbl id) => (contains row)
      (fetch- db kw-tbl -1) => nil)
    (fact "query-"
      (count (query- db tbl {:a 1})) => 1
      (first (query- db tbl {:a 1})) => (contains row)
      (query- db tbl {:a 2}) => nil
      (count (query- db tbl {})) => 1
      (first (query- db tbl {:a 1})) => (contains row)
      (count (query- db tbl nil)) => 1
      (first (query- db tbl nil)) => (contains row)
      (fact "rejects keyword data"
        (query- db tbl row-with-kw) => (throws)))
    (fact "query- with keyword table"
      (count (query- db kw-tbl {:a 1})) => 1
      (first (query- db kw-tbl {:a 1})) => (contains row))
    (fact "update"
      (write- db tbl id row2) => id
      (fetch- db tbl id) => (contains row2))
    (fact "delete-"
      (count (query- db tbl nil)) => 1
      ; Delete nil shouldn't do anything
      (delete- db tbl nil) => anything
      (count (query- db tbl nil)) => 1
      (delete- db tbl id) => anything
      (count (query- db tbl nil)) => 0
      (fact "rejects keyword data"
        (delete- db tbl row-with-kw) => (throws)))
    (fact "query- with empty table"
      (query- db tbl nil) => nil))
  (let [id (write- db tbl nil row)
        idb (write- db tbl nil rowb)]
    (fact "query- with multiple rows"
      (count (query- db tbl {:a 1})) => 2
      (count (query- db tbl {:b 1})) => 1
      (count (query- db tbl nil)) => 2)
    (fact "update with multiple rows"
      (write- db tbl id row2) => id
      (fetch- db tbl id) => (contains row2)
      (fetch- db tbl idb) => (contains rowb))
    (fact "delete- with multiple rows"
      (count (query- db tbl nil)) => 2
      (delete- db tbl id) => anything
      (count (query- db tbl nil)) => 1
      (first (query- db tbl nil)) => (contains rowb))))

(facts "MemDb"
  (test-db (->MemDb (atom {}))))

