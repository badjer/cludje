(ns cludje.crud-test
  (:require [cludje.app :as app]
            [cludje.uiadapter :as ui])
  (:use cludje.core
        cludje.types
        cludje.crud
        cludje.database
        cludje.test
        midje.sweet))

(defn crud-test-sys []
  (app/make-system 
              {:uiadapter (ui/->TestUIAdapter (atom nil))
               :default-action nil
               :action-ns 'cludje.crud-test
               :model-ns 'cludje.crud-test}))


(defmodel Gear {:teeth Int})
(def gear {:teeth 4})

(def-crud-actions Gear)

(fact "def-crud-actions"
  (let [db (->MemDb (atom {}))
        sys {:db db} 
        kees (gear-add sys gear)]
    (count (gear-list sys nil)) => 1
    (first (:gears (gear-list sys nil))) => (contains gear)
    (gear-edit sys kees) => (contains gear)
    (gear-new sys {}) => {}
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


(defmodel Geartype {:name Str :isarchived Bool}
  :defaults {:isarchived false})
(def geartype {:name "A" :isarchived false})

(def-crud-actions Geartype)

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


(defaction ac-with-lookup- (with-lookup- {} Geartype system {}))

(fact "with-lookup-"
  (let [sys (crud-test-sys)]
    (run-action sys geartype-add {:name "A" :isarchived false}) => ok?
    (let [res (run-action sys ac-with-lookup- {})]
      res => (has-keys :geartypes)
      (map :name (:geartypes res)) => ["A"])))

(defaction ac-lookup (with-lookup {} Geartype))

(defaction ac-companyid 1)

(defmodel Widgettype {:companyid Int :name Str}
  :defaults {:companyid #'ac-companyid})
(def-crud-actions Widgettype)

(defaction ac-lookup-fn (with-lookup {} Widgettype))

(fact "with-lookup"
  (let [sys (crud-test-sys)]
    (run-action sys geartype-add {:name "A" :isarchived false}) => ok?
    (let [res (run-action sys ac-lookup {})]
      res => (has-keys :geartypes)
      (map :name (:geartypes res)) => ["A"])
    (fact "using a fn for a default value"
      (run-action sys widgettype-add {:name "A" :companyid 1}) => ok?
      (let [sres (run-action sys ac-lookup-fn {})]
        sres => ok?
        sres => (has-keys :widgettypes)
        (map :name (:widgettypes sres)) => ["A"]))))


(defmodel Sprockettype {:companyid Int :name Str}
  :defaults {:companyid #'ac-companyid}
  :partitions [:companyid])

(def-crud-actions Sprockettype)

(fact "def-crud-actions sets defaults with a func"
  (let [db (->MemDb (atom {}))
        sys {:db db}]
    (sprockettype-new sys {}) => (contains {:companyid 1})))

(fact "partitions makes model-list include selector"
  (let [sys (crud-test-sys)] 
    (run-action sys sprockettype-add {:name "A" :companyid 1}) => ok?
    (run-action sys sprockettype-add {:name "B" :companyid 2}) => ok?
    (let [sres1 (run-action sys sprockettype-list {:companyid 1})
          sres2 (run-action sys sprockettype-list {:companyid 2})]
      sres1 => ok?
      sres2 => ok?
      (map :name (:sprockettypes sres1)) => ["A"]
      (map :name (:sprockettypes sres2)) => ["B"]
      (fact "with the wrong type supplied"
        (let [sres-w (run-action sys sprockettype-list {:companyid "1"})]
          (map :name (:sprockettypes sres-w)) => ["A"])))
    (fact "uses default if none supplied"
      (let [sres-d (run-action sys sprockettype-list {})]
        sres-d => ok?
        (map :name (:sprockettypes sres-d)) => ["A"]))))

(defmodel Footype {:companyid Str :name Str}
  :defaults {:companyid 1}
  :partitions [:companyid])

(def-crud-actions Footype)

(fact "partitions makes model-list include selector default - default is wrong type"
  (let [sys (crud-test-sys)]
    (run-action sys footype-add {:name "A" :companyid "1"}) => ok?
    (run-action sys footype-add {:name "Z" :companyid "2"}) => ok?
    (let [res (run-action sys footype-list {})]
      (map :name (:footypes res)) => ["A"])))


