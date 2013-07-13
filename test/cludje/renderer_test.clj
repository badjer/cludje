(ns cludje.renderer-test
  (:use midje.sweet
        cludje.test
        cludje.core
        cludje.renderer))

(fact "LiteralRenderer"
  (let [renderer (->LiteralRenderer)]
    (render- renderer nil {:a 1}) => {:a 1}
    (render- renderer nil nil) => nil))

(fact "JsonRenderer"
  (let [renderer (->JsonRenderer)]
    ; We don't need to test all sorts of JSON, that's the 
    ; responsibility of our JSON lib
    ; Just make sure it works at least a little bit
    (:body (render- renderer nil {:a 1 :b 2})) => "{\"a\":1,\"b\":2}"))

