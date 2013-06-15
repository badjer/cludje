(ns cludje.types-test
  (:use midje.sweet
        cludje.types))

(fact "Str"
  (show Str "a") => "a"
  (parse Str "a") => "a"
  (validate Str "a") => true)

(fact "Email"
  (let [em "a@bc.de"]
    (show Email em) => em
    (parse Email em) => em
    (validate Email em) => true
    (validate Email "a") => false))

(fact "Password"
  (let [pw "1234567890"]
    (show Password pw) => "********"
    (parse Password pw) => pw
    (validate Password pw) => true
    (validate Password "1") => false))

