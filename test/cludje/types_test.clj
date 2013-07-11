(ns cludje.types-test
  (:use midje.sweet
        cludje.test
        cludje.types))

(fact "value?"
  (value? nil) => falsey
  (value? "") => falsey
  (value? "a") => truthy
  (value? 1) => truthy)

(fact "needs should return an entry for each missing field"
  (needs {} :a) => (has-keys :a)
  (needs {} :a :b) => (has-keys :a :b)
  (needs {:a 1} :a) => falsey
  (needs {:a ""} :a) => (has-keys :a)
  (needs {:a nil} :a) => (has-keys :a)
  (needs {:a 1} :b) => (has-keys :b)
  (needs {:a 1 :b 2} :a :b) => falsey)

(fact "bad should work with a cludje type"
  (bad Email "a") => truthy
  (bad Email "a@b.cd") => falsey)

(fact "bad should work with a fn"
  (bad even? 1) => truthy
  (bad even? 2) => falsey)

(fact "bad should return truthy only if the value? and not the given test"
  (bad Email nil) => falsey
  (bad Email "") => falsey
  (bad Email "a") => truthy
  (bad Email "a@b.cd") => falsey)

(fact "no-bad should return an entry for each field that 
      isn't null and is invlid"
  (no-bad Email {} :a) => falsey
  (no-bad Email {:a ""} :a) => falsey
  (no-bad Email {:a "a"} :a) => (has-keys :a)
  (no-bad Email {:a "a@b.cd"} :a) => falsey)

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

(fact "Bool"
  (let [true-vals [true "true" "True" "t" "T" "yes" "Yes" "y" "Y" 1]
        false-vals [false "false" "False" "f" "F" "no" "No" "n" "N" 0]]
    (show Bool true) => "yes"
    (show Bool false) => "no"
    (for [v true-vals]
      (parse Bool v) => true)
    (for [v false-vals]
      (parse Bool v) => false)
    (for [v [nil ""]]
      (parse Bool v) => nil)
    (for [v (concat true-vals false-vals)]
      (validate Bool v) => truthy)
    (for [v ["asdf" -1 2]]
      (validate Bool v) => falsey)))


(def feb20 1361318400000)

(fact "Date"
  (parse Date "1970-01-01") => 0 
  (parse Date "1970-01-02") => 86400000 
  (parse Date "2013-02-20") => feb20
  (parse Date "") => nil
  (parse Date nil) => nil
  (show Date feb20) => "Wed Feb 20" 
  (show Date nil) => nil 
  (show Date (+ feb20 10000)) => "Wed Feb 20"
  (facts "show Date with different year shows year" 
    (show Date (parse Date "2008-01-21")) => "Jan 21, 2008")
  (validate Date nil) => truthy
  (validate Date "") => truthy
  (validate Date feb20) => truthy
  (validate Date "2013-02-20") => truthy
  (validate Date "abc") => falsey)

(def oh-one-am 60000)
(def one-oh-one-pm 46860000)

(fact "Time"
  (parse Time nil) => nil
  (parse Time "") => nil
  (parse Time oh-one-am) => oh-one-am
  (parse Time one-oh-one-pm) => one-oh-one-pm
  (parse Time "0:01 AM") => oh-one-am
  (parse Time "1:01 PM") => one-oh-one-pm
  (parse Time "00:00 AM") => 0
  (parse Time "00:01 AM") => 60000
  (parse Time "01:00 AM") => 3600000
  (parse Time "12:00 PM") => 43200000
  (parse Time "12:00 AM") => 0
  (parse Time "01:01 AM") => 3660000
  (parse Time "01:00 PM") => 46800000
  (parse Time "01:01 PM") => 46860000
  (parse Time "1:00 AM") => 3600000
  (parse Time (+ oh-one-am feb20)) => (+ feb20 oh-one-am)
  (show Time oh-one-am) => "12:01 AM"
  (show Time one-oh-one-pm) => "01:01 PM"
  (show Time (+ feb20 oh-one-am)) => "12:01 AM"
  (validate Time nil) => truthy
  (validate Time "") => truthy
  (validate Time oh-one-am) => truthy
  (validate Time one-oh-one-pm) => truthy
  (validate Time "abc") => falsey)

(def one-min 60000)
(def fifteen-min (* 15 one-min))
(def thirteen-hour 46800000)
(def thirteen-oh-one (+ thirteen-hour one-min))

(fact "Timespan"
  (parse Timespan nil) => nil
  (parse Timespan "") => nil
  (parse Timespan one-min) => one-min
  (parse Timespan thirteen-oh-one) => thirteen-oh-one
  (parse Timespan fifteen-min) => fifteen-min
  (parse Timespan "1") => (* 60 one-min)
  (parse Timespan "13") => thirteen-hour
  (parse Timespan "13.25") => (+ thirteen-hour fifteen-min)
  (parse Timespan ".25") => fifteen-min
  (parse Timespan "0.25") => fifteen-min
  (show Timespan one-min) => "0.01"
  (show Timespan thirteen-oh-one) => "13.01"
  (show Timespan fifteen-min) => "0.25"
  (show Timespan thirteen-hour) => "13.00"
  (show Timespan (+ thirteen-hour fifteen-min)) => "13.25"
  (validate Timespan nil) => truthy
  (validate Timespan "") => truthy
  (validate Timespan one-min) => truthy
  (validate Timespan "1") => truthy
  (validate Timespan ".25") => truthy
  (validate Timespan "0.25") => truthy
  (validate Timespan "abc") => falsey)

(fact "DateTime"
  (parse DateTime nil) => nil
  (parse DateTime "") => nil
  (parse DateTime oh-one-am) => oh-one-am
  (parse DateTime one-oh-one-pm) => one-oh-one-pm
  (parse DateTime "0:01 AM") => oh-one-am
  (parse DateTime "1:01 PM") => one-oh-one-pm
  (parse DateTime "00:00 AM") => 0
  (parse DateTime "00:01 AM") => 60000
  (parse DateTime "01:00 AM") => 3600000
  (parse DateTime "12:00 PM") => 43200000
  (parse DateTime "12:00 AM") => 0
  (parse DateTime "01:01 AM") => 3660000
  (parse DateTime "01:00 PM") => 46800000
  (parse DateTime "01:01 PM") => 46860000
  (parse DateTime "1:00 AM") => 3600000
  (parse DateTime (+ oh-one-am feb20)) => (+ feb20 oh-one-am)
  (show DateTime oh-one-am) => "12:01 AM"
  (show DateTime one-oh-one-pm) => "01:01 PM"
  (show DateTime (+ feb20 oh-one-am)) => "12:01 AM"
  (validate DateTime nil) => truthy
  (validate DateTime "") => truthy
  (validate DateTime oh-one-am) => truthy
  (validate DateTime one-oh-one-pm) => truthy
  (validate DateTime "abc") => falsey)
