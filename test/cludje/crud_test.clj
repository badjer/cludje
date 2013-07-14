(ns cludje.crud-test
  (:use cludje.core
        cludje.types
        cludje.crud
        cludje.database
        midje.sweet))

(defmodel Gear {:teeth Int})
(def gear {:teeth 4})

(def-crud-actions Gear)

(fact "def-crud-actions"
  (let [db (->MemDb (atom {}))
        sys {:db db} 
        id (post-gear sys gear)]
    (count (list-gear sys nil)) => 1
    (first (list-gear sys nil)) => (contains gear)
    (put-gear sys {:_id id :teeth 5}) => anything
    (get-gear sys {:_id id}) => (contains {:teeth 5})
    (delete-gear sys {:_id id}) => anything
    (list-gear sys nil) => empty?))

