(ns cludje.util-test
  (:use midje.sweet
        cludje.test
        cludje.types
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

(defn is-?-like [f ex-checker]
  (facts "is-?-like"
    (f {:a 1} :a) => 1
    (f {:a 1} :b) => (ex-checker)
    (f {:a {:b 1}} [:a :b]) => 1
    (f {:a {:b 1}} [:a :z]) => (ex-checker)
    (facts "with nil values"
      (f {:a nil} :a) => (ex-checker)
      (f {:a {:b nil}} [:a :b]) => (ex-checker))))

(facts "?"
  (is-?-like ? throws-problems)
  (? nil :a) => (throws-error))

(facts "?!"
  (is-?-like ?! throws-error)
  (?! nil :a) => (throws-error))

(facts "??"
  (?? {:a 1} :a) => 1
  (?? {:a 1} :b) => nil
  (?? {:a {:b 1}} [:a :b]) => 1
  (?? {:a {:b 1}} [:a :z]) => nil
  (?? {:a {:b 1}} [:z :y]) => nil
  (fact "with nil values"
    (?? {:a nil} :a) => nil
    (?? {:a {:b nil}} [:a :b]) => nil))

(facts "&?"
  (&? {:a 1} :a) => 1
  (&? {:a 1} :b :a) => 1
  (&? {:a 1}) => (throws)
  (&? {:a 1} :b) => (throws)
  (&? {:a {:b 1}} [:a :b]) => 1
  (&? {:a {:b 1}} [:a :z]) => (throws)
  (&? {:a {:b 1}} [:a :z] [:a :b]) => 1
  (&? {:a {:b 1}} [:a :z] [:a :y]) => (throws)
  (fact "with nil values"
    (&? {:a nil} :a) => (throws)
    (&? {:a nil} :b :a) => (throws)
    (&? {:a {:b nil}} [:a :b]) => (throws)))

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

(fact "today"
  (today 1970 1 1) => 0
  (today 2013 9 15) => 1379203200000)

(fact "with-optional-param"
  (with-optional-param {} {:a 1} :a Int) => {:a 1}
  (with-optional-param {} {} :a Int) => {}
  (with-optional-param {} {:a "1"} :a Int) => {:a 1}
  (with-optional-param {:z 1} {:a "1"} :a Int) => {:a 1 :z 1}
  (with-optional-param {:z 1} {} :a Int) => {:z 1}
  (fact "without map"
    (with-optional-param {:a 1} :a Int) => {:a 1}
    (with-optional-param {} :a Int) => {}
    (with-optional-param {:a "1"} :a Int) => {:a 1})
  )

(fact "with-param"
  (with-param {} {:a 1} :a 0 Int) => {:a 1}
  (with-param {} {} :a 0 Int) => {:a 0}
  (with-param {} {:a "1"} :a 0 Int) => {:a 1}
  (with-param {} {} :a "0" Int) => {:a 0}
  (with-param {:z 1} {:a 1} :a 0 Int) => {:a 1 :z 1}
  (with-param {:z 1} {} :a 0 Int) => {:a 0 :z 1}
  (fact "without map"
    (with-param {:a 1} :a 0 Int) => {:a 1}
    (with-param {} :a 0 Int) => {:a 0}
    (with-param {:a "1"} :a 0 Int) => {:a 1}
    (with-param {} :a "0" Int) => {:a 0})
  )
