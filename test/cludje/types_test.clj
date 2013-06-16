(ns cludje.types-test
  (:use midje.sweet
        cludje.types))

(fact "Str"
  (show Str "a") => "a"
  (parse Str "a") => "a"
  (parse Str 123) => "123"
  ; Empty values are valid
  (parse Str nil) => nil
  (parse Str "") => ""
  (validate Str "") => true
  (validate Str 123) => true
  (validate Str nil) => true
  (validate Str "a") => true)

(fact "Email"
  (let [em "a@bc.de"]
    (show Email em) => em
    (parse Email em) => em
    ;Empty values are valid
    (parse Email nil) => nil
    (parse Email "") => ""
    (validate Email nil) => truthy
    (validate Email "") => truthy
    (validate Email em) => truthy
    (validate Email "a") => falsey))

(fact "Password"
  (let [pw "1234567890"]
    (show Password pw) => ""
    (parse Password pw) => pw
    ; Empty values are valid
    (parse Password nil) => nil
    (parse Password "") => ""
    (validate Password nil) => true
    (validate Password "") => true
    (validate Password pw) => true
    (validate Password "1") => false))

(fact "Int"
  (let [s "123"
        i 123]
    (show Int i) => s
    (show Int s) => s
    (parse Int s) => i
    (parse Int i) => i
    ; Empty values are valid
    (parse Int nil) => nil
    (parse Int "") => nil
    (validate Int nil) => truthy
    (validate Int "") => truthy
    (validate Int "asdf") => falsey
    (validate Int i) => truthy
    (validate Int s) => truthy))

(fact "Money"
  (let [ds "$123"
        dv 12300
        dcs "$123.45"
        dcss "123.45"
        dcv 12345
        cs "$0.67"
        cv 67]
    (show Money dv) => ds
    (show Money dcv) => dcs
    (parse Money ds) => dv
    (parse Money dv) => dv
    (parse Money dcs) => dcv
    ; It should work without the dollar sign too
    (parse Money dcss) => dcv
    (parse Money dcv) => dcv
    (parse Money cs) => cv
    (parse Money cv) => cv
    ; Empty values are valid
    (parse Money nil) => nil
    (parse Money "") => nil
    (validate Money nil) => truthy
    (validate Money "") => truthy
    (validate Money "asdf") => falsey
    (validate Money ds) => truthy
    (validate Money dv) => truthy
    (validate Money dcs) => truthy
    (validate Money cs) => truthy))


