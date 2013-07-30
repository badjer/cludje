(ns cludje.crud-test
  (:require [cludje.app :as app]
            [cludje.uiadapter :as ui])
  (:use cludje.core
        cludje.types
        cludje.crud
        cludje.database
        cludje.test
        midje.sweet))

(defmodel Gear {:teeth Int})
(def gear {:teeth 4})

(def-crud-actions Gear)

(fact "def-crud-actions"
  (let [db (->MemDb (atom {}))
        sys {:db db} 
        kees (gear-add sys gear)]
    (count (gear-list sys nil)) => 1
    (first (:gears (gear-list sys nil))) => (contains gear)
    (gear-alter sys (assoc kees :teeth 5)) => anything
    (gear-show sys kees) => (contains {:teeth 5})
    (gear-delete sys kees) => anything
    (:gears (gear-list sys nil)) => empty?))

(defmodel Widget {:teeth Int :size Int}
  :defaults {:size 3})
(def-crud-actions Widget)

(fact "def-crud-actions sets defaults when calling new"
  (let [db (->MemDb (atom {}))
        sys {:db db}]
    (widget-new sys nil) => (contains {:size 3})))


(defmodel-lookup GearType)
(def geartype {:name "A" :isarchived false})

(def-crud-actions GearType)

(fact "def-crud-actions with lookup model"
  (let [db (->MemDb (atom {}))
        sys {:db db}
        kees (geartype-add sys geartype)]
    (fact "-new automatically sets isarchived"
      (geartype-new sys nil) => (contains {:isarchived false}))
    (fact "can add without isarchived"
      kees => ok?)
    (fact "can list"
      (count (:geartypes (geartype-list sys {:isarchived false}))) => 1
      (first (:geartypes (geartype-list sys {:isarchived false}))) => 
        (contains geartype))))


(defaction ac-with-lookup- (with-lookup- {} system GearType))

(fact "with-lookup-"
  (let [sys (app/make-system 
              {:uiadapter (ui/->TestUIAdapter (atom nil))
               :default-action nil
               :action-ns 'cludje.crud-test
               :model-ns 'cludje.crud-test})]
    (run-action sys geartype-add {:name "A" :isarchived false}) => ok?
    (let [res (run-action sys ac-with-lookup- {})]
      res => (has-keys :geartypes)
      (map :name (:geartypes res)) => ["A"])))

(defaction ac-lookup (with-lookup {} GearType))

(fact "with-lookup"
  (let [sys (app/make-system 
              {:uiadapter (ui/->TestUIAdapter (atom nil))
               :default-action nil
               :action-ns 'cludje.crud-test
               :model-ns 'cludje.crud-test})]
    (run-action sys geartype-add {:name "A" :isarchived false}) => ok?
    (let [res (run-action sys ac-lookup {})]
      res => (has-keys :geartypes)
      (map :name (:geartypes res)) => ["A"])))


