(ns cludje.util-test
  (:use midje.sweet
        cludje.test
        cludje.util))

(facts "map-vals"
  (map-vals {:a 1 :b 1} inc) => {:a 2 :b 2})

(defn getx [] "x")

(fact "realize"
  (fact "with val"
    (realize 1) => 1)
  (fact "with fn"
    (realize getx) => "x")
  (fact "within map-vals"
    (map-vals {:a getx :b 1} realize) => {:a "x" :b 1}))

(facts "?"
  (? {:a 1} :a) => 1
  (? {:a 1} :b) => (throws)
  (? {:a {:b 1}} [:a :b]) => 1
  (? {:a {:b 1}} [:a :z]) => (throws))

(facts "??"
  (?? {:a 1} :a) => 1
  (?? {:a 1} :b) => nil
  (?? {:a {:b 1}} [:a :b]) => 1
  (?? {:a {:b 1}} [:a :z]) => nil)

(facts "&?"
  (&? {:a 1} :a) => 1
  (&? {:a 1} :b :a) => 1
  (&? {:a 1}) => (throws)
  (&? {:a 1} :b) => (throws)
  (&? {:a {:b 1}} [:a :b]) => 1
  (&? {:a {:b 1}} [:a :z]) => (throws)
  (&? {:a {:b 1}} [:a :z] [:a :b]) => 1
  (&? {:a {:b 1}} [:a :z] [:a :y]) => (throws))

(fact "with-problem"
  (-> {} (with-problem :a "err")) => {:__problems {:a "err"}}
  (-> nil (with-problem :a "err")) => {:__problems {:a "err"}}
  (-> {:a 1} (with-problem :a "err")) => {:a 1 :__problems {:a "err"}}
  (-> {} (with-problem :a "err")) => (has-problems :a))

(fact "with-alert"
  (-> {} (with-alert :info "hi")) => {:__alerts [{:type :info :text "hi"}]}
  (-> nil (with-alert :info "hi")) => {:__alerts [{:type :info :text "hi"}]}
  (-> {:a 1} (with-alert :info "hi")) => 
    {:a 1 :__alerts [{:type :info :text "hi"}]}
  (-> {} (with-alert :info "hi")) => (has-alert :info #"hi"))

