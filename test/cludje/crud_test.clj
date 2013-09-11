(ns cludje.crud-test
  (:use cludje.action
        cludje.types
        cludje.model
        cludje.crud
        cludje.datastore
        cludje.actionfind
        cludje.test
        midje.sweet))

(def Gear (>Model {:teeth Int} {:modelname "gear"}))

(def gear {:teeth 4})

(def action-finder (>NSActionFinder 'cludje.crud-test))

(defn >sys-request []
  {:system {:data-store (>TestDatastore)
            :action-finder action-finder}})

(defn >in [request input] 
  (-> request
      (assoc :input input)))


(fact "crud actions"
  (let [request (>sys-request)
        kees (crud-model-add Gear (>in request gear))]
    kees => (has-keys :_id)
    (count (crud-model-list Gear request)) => 1
    (first (:gears (crud-model-list Gear request))) => (contains gear)
    (crud-model-edit Gear (>in request kees)) => (contains gear)
    (crud-model-new Gear request) => {}
    (crud-model-alter Gear (>in request (assoc kees :teeth 5))) => map?
    (crud-model-show Gear (>in request kees)) => (contains {:teeth 5})
    (crud-model-delete Gear (>in request kees)) => nil
    (:gears (crud-model-list Gear request)) => empty?
    ))

(def Widget (>Model {:teeth Int :size Int}
                    {:modelname "widget" :defaults {:size 3}}))
(def-crud-actions Widget)

(defn ac-companyid [request] 1)
(def Widgettype (>Model {:companyid Int :name Str}
                        {:modelname "widgettype" :defaults {:companyid ac-companyid}}))

(fact "model-defaults"
  (model-defaults Widget {}) => {:size 3}
  (fact "with function"
    (model-defaults Widgettype {}) => {:companyid 1})
  (fact "ignores input"
    (model-defaults Widget {:input {:size 1}}) => {:size 3}))

(fact "build"
  (build Gear {:input {:teeth 3}}) => {:_id nil :teeth 3}
  (fact "with defaults"
    (fact "no input override"
      (build Widget {:input {:teeth 1}}) => (contains {:size 3 :teeth 1}))
    (fact "input override"
      (build Widget {:input {:teeth 1 :size 2}}) => (contains {:size 2 :teeth 1}))))

(def Part (>Model {:size Int} {:modelname "part" 
                               :partitions [:size]}))
(def Part-default (>Model {:size Int} {:modelname "part-default"
                                       :partitions [:size]
                                       :defaults {:size 1}}))
(def Part-default-fn (>Model {:size Int} {:modelname "part-default-fn"
                                       :partitions [:size]
                                       :defaults {:size ac-companyid}}))

(fact "list-params"
  (fact "defaults to nothing"
    (list-params Gear {}) => {})
  (fact "ignores defaults"
    (list-params Widget {}) => {})
  (fact "with partitions"
    (list-params Part {:input {:size 1}}) => {:size 1}
    (fact "if not supplied, throw problems"
      (list-params Part {:input {}}) => (throws-problems))
    (fact "and defaults"
      (list-params Part-default {:input {}}) => {:size 1}
      (fact "and default function"
        (list-params Part-default-fn {:input {}}) => {:size 1}))))





(def-crud-actions Widgettype)

(fact "crud defaults"
  (let [request (>sys-request)]
    (fact "new sets defaults"
      (new-widget request) => (contains {:size 3}))
    (fact "add sets defaults"
      (let [id (add-widget (>in request {:teeth 2}))]
        id => ok?
        (show-widget (>in request id)) => (contains {:teeth 2 :size 3})))
    (fact "set defaults with fn"
      (new-widgettype (>sys-request)) => (contains {:companyid 1}))))


(def Geartype (>Model {:name Str :isarchived Bool}
                      {:modelname "geartype" :defaults {:isarchived false}}))
(def geartype {:name "A" :isarchived false})

(def-crud-actions Geartype)

(fact "def-crud-actions with lookup model"
  (let [request (>sys-request)
        kees (add-geartype (>in request geartype))]
    (fact "new- automatically sets isarchived"
      (new-geartype request) => (contains {:isarchived false}))
    (fact "can add without isarchived"
      kees => ok?)
    (fact "can list"
      (count (:geartypes (list-geartype (>in request {:isarchived false})))) => 1
      (first (:geartypes (list-geartype (>in request {:isarchived false})))) => 
        (contains geartype))))


(defn ac-with-lookup [request]
  (with-lookup request {} Geartype))

(defn ac-lookup-fn [request]
  (with-lookup request {} Widgettype))

(fact "with-lookup"
  (let [request (>sys-request)]
    ; Set up our test data
    (add-geartype (>in request {:name "A"})) => ok?
    (let [res (ac-with-lookup request)]
      res => (has-keys :geartypes)
      (map :name (:geartypes res)) => ["A"])
    (fact "using a fn for a default value" 
      (let [_ (add-widgettype (>in request {:name "A"}))
            sres (ac-lookup-fn request)]
        sres => ok?
        sres => (has-keys :widgettypes)
        (map :name (:widgettypes sres)) => ["A"]))))



(def Sprockettype (>Model {:companyid Int :name Str}
                          {:modelname "sprockettype"
                           :defaults {:companyid #'ac-companyid} 
                           :partitions [:companyid]}))

(def-crud-actions Sprockettype)

(fact "partitions makes model-list include selector"
  (let [request (>sys-request)]
    (add-sprockettype (>in request {:name "A" :companyid 1})) => ok?
    (add-sprockettype (>in request {:name "B" :companyid 2})) => ok?
    (let [sres1 (list-sprockettype (>in request {:companyid 1})) 
          sres2 (list-sprockettype (>in request {:companyid 2}))]
      sres1 => ok?
      sres2 => ok?
      (map :name (:sprockettypes sres1)) => ["A"]
      (map :name (:sprockettypes sres2)) => ["B"])
    (fact "uses default if none supplied"
      (let [sres-d (list-sprockettype request)]
        sres-d => ok?
        (map :name (:sprockettypes sres-d)) => ["A"]))))

(def Footype (>Model 
                     {:companyid Str :name Str}
                     {:modelname "footype"
                      :defaults {:companyid 1} 
                      :partitions [:companyid]}))

(def-crud-actions Footype)

(fact "partitions makes model-list include selector default - default is wrong type"
  (let [request (>sys-request)]
    (add-footype (>in request {:name "A" :companyid "1"})) => ok?
    (add-footype (>in request {:name "B" :companyid "2"})) => ok?
    (let [res (list-footype request)]
      (map :name (:footypes res)) => ["A"])))


(facts "with-crud-dsl"
  (let [request (assoc (>sys-request) :input {:teeth 4})]
  (with-crud-dsl request
    (fact "defines with-lookup"
      (with-lookup {} Widget) =not=> (throws))
    (fact "defines build"
      (build Gear) =not=> (throws))
    (fact "defines create"
      (create Gear) =not=> (throws)))))

