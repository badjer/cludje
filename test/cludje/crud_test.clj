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
        id (add-gear sys gear)]
    (count (list-gears sys nil)) => 1
    (first (list-gears sys nil)) => (contains gear)
    (edit-gear sys {:_id id :teeth 5}) => anything
    (get-gear sys {:_id id}) => (contains {:teeth 5})
    (delete-gear sys {:_id id}) => anything
    (list-gears sys nil) => empty?))
