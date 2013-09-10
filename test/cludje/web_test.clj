(ns cludje.web-test
  (:use midje.sweet
        cludje.test
        cludje.web))

(fact "wrap-ring-middleware"
  (let [handler (wrap-ring-middleware identity)]
    (fact "returns a fn"
      handler => fn?)
    (fact "adds :params"
      (fact "from query-string"
        (handler {:query-string "a=1"}) => (contains {:params {:a "1"}})))))
