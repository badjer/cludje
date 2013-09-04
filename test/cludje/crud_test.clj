(ns cludje.crud-test
  (:use cludje.action
        cludje.types
        cludje.model
        cludje.crud
        cludje.datastore
        cludje.actionfind
        cludje.test
        midje.sweet))

(def Gear (>Model "gear" {:teeth Int} {}))

(def gear {:teeth 4})


(defn >context [moldsym]
  {:system {:data-store (>TestDatastore)} :input-mold-sym moldsym})

(defn >in [context input] 
  (-> context
      (assoc :input input)))

(fact "crud actions"
  (let [context (>context `Gear)
        kees (crud-model-add (>in context gear))]
    kees => (has-keys :_id)
    (count (crud-model-list context)) => 1
    (first (:gears (crud-model-list context))) => (contains gear)
    (crud-model-edit (>in context kees)) => (contains gear)
    (crud-model-new context) => {}
    (crud-model-alter (>in context (assoc kees :teeth 5))) => map?
    (crud-model-show (>in context kees)) => (contains {:teeth 5})
    (crud-model-delete (>in context kees)) => nil
    (:gears (crud-model-list context)) => empty?
    ))

(def Widget (>Model "widget" {:teeth Int :size Int}
                    {:defaults {:size 3}}))
(def-crud-actions Widget)

(defn ac-companyid [context] 1)
(def Widgettype (>Model "widgettype" {:companyid Int :name Str}
                        {:defaults {:companyid ac-companyid}}))
(def-crud-actions Widgettype)

(fact "crud defaults"
  (let [context (>context `Widget)]
    (fact "new sets defaults"
      (new-widget context) => (contains {:size 3}))
    (fact "add sets defaults"
      (let [id (add-widget (>in context {:teeth 2}))]
        id => ok?
        (show-widget (>in context id)) => (contains {:teeth 2 :size 3})))
    (fact "set defaults with fn"
      (new-widgettype (>context `Widgettype)) => (contains {:companyid 1}))))


(def Geartype (>Model "geartype" {:name Str :isarchived Bool}
                      {:defaults {:isarchived false}}))
(def geartype {:name "A" :isarchived false})

(def-crud-actions Geartype)

(fact "def-crud-actions with lookup model"
  (let [context (>context `Geartype)
        kees (add-geartype (>in context geartype))]
    (fact "new- automatically sets isarchived"
      (new-geartype context) => (contains {:isarchived false}))
    (fact "can add without isarchived"
      kees => ok?)
    (fact "can list"
      (count (:geartypes (list-geartype (>in context {:isarchived false})))) => 1
      (first (:geartypes (list-geartype (>in context {:isarchived false})))) => 
        (contains geartype))))


(defn ac-with-lookup [context]
  (with-lookup context {} Geartype))

(defn ac-lookup-fn [context]
  (with-lookup context {} Widgettype))

(fact "with-lookup"
  (let [action-finder (>NSActionFinder 'cludje.crud-test)
        context (assoc-in (>context `Geartype) 
                          [:system :action-finder] action-finder)]
    ; Set up our test data
    (add-geartype (>in context {:name "A"})) => ok?
    (let [res (ac-with-lookup context)]
      res => (has-keys :geartypes)
      (map :name (:geartypes res)) => ["A"])
    (fact "using a fn for a default value" 
      (let [context (assoc context :input-mold-sym `Widgettype)
            _ (add-widgettype (>in context {:name "A"}))
            sres (ac-lookup-fn context)]
        sres => ok?
        sres => (has-keys :widgettypes)
        (map :name (:widgettypes sres)) => ["A"]))))



(def Sprockettype (>Model "sprockettype" 
                          {:companyid Int :name Str}
                          {:defaults {:companyid #'ac-companyid} 
                           :partitions [:companyid]}))

(def-crud-actions Sprockettype)

(fact "partitions makes model-list include selector"
  (let [context (>context `Sprockettype)]
    (add-sprockettype (>in context {:name "A" :companyid 1})) => ok?
    (add-sprockettype (>in context {:name "B" :companyid 2})) => ok?
    (let [sres1 (list-sprockettype (>in context {:companyid 1})) 
          sres2 (list-sprockettype (>in context {:companyid 2}))]
      sres1 => ok?
      sres2 => ok?
      (map :name (:sprockettypes sres1)) => ["A"]
      (map :name (:sprockettypes sres2)) => ["B"])
    (fact "uses default if none supplied"
      (let [sres-d (list-sprockettype context)]
        sres-d => ok?
        (map :name (:sprockettypes sres-d)) => ["A"]))))

(def Footype (>Model "footype" 
                     {:companyid Str :name Str}
                     {:defaults {:companyid 1} 
                      :partitions [:companyid]}))

(def-crud-actions Footype)

(fact "partitions makes model-list include selector default - default is wrong type"
  (let [context (>context `Footype)]
    (add-footype (>in context {:name "A" :companyid "1"})) => ok?
    (add-footype (>in context {:name "B" :companyid "2"})) => ok?
    (let [res (list-footype context)]
      (map :name (:footypes res)) => ["A"])))


