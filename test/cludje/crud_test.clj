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
        id (gear-new sys gear)]
    (count (gear-list sys nil)) => 1
    (first (:gears (gear-list sys nil))) => (contains gear)
    (gear-edit sys {:_id id :teeth 5}) => anything
    (gear-show sys {:_id id}) => (contains {:teeth 5})
    (gear-delete sys {:_id id}) => anything
    (:gears (gear-list sys nil)) => empty?))

