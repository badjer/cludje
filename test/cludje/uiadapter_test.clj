(ns cludje.uiadapter-test
  (:use midje.sweet
        cludje.test
        cludje.core
        cludje.uiadapter))


(fact "TestUIAdapter"
  (let [ui (->TestUIAdapter (atom nil))]
    (render- ui nil {:a 1}) => {:a 1}
    (render- ui nil nil) => nil
    (fact "stores persistent fields"
      (render- ui nil {:_p_a 1}) => anything
      (parse-input- ui {}) => {:_p_a 1})))

(fact "WebUIAdapter"
  (let [ui (->WebUIAdapter true)]
    ; We don't need to test all sorts of JSON, that's the 
    ; responsibility of our JSON lib
    ; Just make sure it works at least a little bit
    (:body (render- ui nil {:a 1 :b 2})) => "{\"a\":1,\"b\":2}" 
    (parse-input- ui {:uri "/api" :params {:a 1}}) => {:a 1}
    (fact "WebUIAdapter sets and reads auth cookie"
      (render- ui nil {:a 1 :_p_cludjeauthtoken "abc"}) => 
        (contains {:cookies {"_p_cludjeauthtoken" {:value "abc"}}})
      (parse-input- ui {:uri "/api" :params {:a 1} 
                       :cookies {"_p_cludjeauthtoken" {:value "b"}}}) => 
        (contains {:_p_cludjeauthtoken "b"}))
    (fact "Returns nil if it's the wrong uri"
      (parse-input- ui {:uri "/foobar" :params {:a 1}}) => nil)
    (fact "Strips off any fields in input that start with __"
      (parse-input- ui {:uri "/api" :params {:__alerts ["a"] :a 1}}) => {:a 1})))

