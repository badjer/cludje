(ns cludje.types-test
  (:use clojure.test
        cludje.types))

(deftest Str-test
  (testing "show"
    (is (= (show Str "a") "a")))
  (testing "parse"
    (is (= (parse Str "a") "a")))
  (testing "validate"
    (is (= (validate Str "a") true))))

(deftest Email-test
  (let [em "a@bc.de"]
    (testing "show"
      (is (= (show Email em) em)))
    (testing "parse"
      (is (= (parse Email em) em)))
    (testing "validate"
      (is (= (validate Email em) true))
      (is (not= (validate Email "a") true)))))

(deftest Password-test
  (let [pw "1234567890"]
    (testing "show"
      (is (= (show Password pw) "********")))
    (testing "parse"
      (is (= (parse Password pw) pw)))
    (testing "validate"
      (is (= (validate Password pw) true))
      (is (not= (validate Password "1") true)))))





