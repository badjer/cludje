(ns cludje.types-test
  (:use midje.sweet
        cludje.test
        cludje.types))

(fact "value?"
  (value? nil) => falsey
  (value? "") => falsey
  (value? "a") => truthy
  (value? 1) => truthy
  (value? false) => truthy)

(fact "Str"
  (show Str "a") => "a"
  (show Str :a) => "a"
  (parse Str "a") => "a"
  (parse Str 123) => "123"
  (parse Str :abc) => "abc"
  ; Empty values are valid
  (parse Str nil) => nil
  (parse Str "") => ""
  (validate Str "") => true
  (validate Str 123) => true
  (validate Str :a) => true
  (validate Str nil) => true
  (validate Str "a") => true
  (validate Str ["a"]) => false
  (validate Str {}) => false
  (validate Str []) => false)


(fact "Email"
  (let [em "a@bc.de"]
    (show Email em) => em
    (parse Email em) => em
    ; Illegal values give nil
    (parse Email "asdf") => nil
    (parse Email 123) => nil
    (parse Email true) => nil
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
    ; Illegal values give nil
    (parse Password 1) => nil
    (parse Password false) => nil
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
    ; Illegal values give nil
    (parse Int "asdf") => nil
    (parse Int true) => nil
    ; Empty values are valid
    (parse Int nil) => nil
    (parse Int "") => nil
    (validate Int nil) => truthy
    (validate Int "") => truthy
    (validate Int "asdf") => falsey
    (validate Int i) => truthy
    (validate Int s) => truthy)

  (fact "works with bigint"
    (let [bi 2N]
      (type bi) => clojure.lang.BigInt
      (parse Int bi) => 2
      (type (parse Int bi)) =not=> clojure.lang.BigInt)))

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
    ; Illegal values give nil
    (parse Money "asdf") => nil
    (parse Money true) => nil
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
    (for [v [nil "" "asdf" 123]]
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
  ; Illegal values give nil
  (parse Date "asdf") => nil
  (parse Date true) => nil
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
  ; Illegal values give nil
  (parse Time "adf") => nil
  (parse Time true) => nil
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
  ; Illegal values give nil
  (parse Timespan "adf") => nil
  (parse Timespan true) => nil
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
  ; Illegal values give nil
  (parse DateTime "adfs") => nil
  (parse DateTime true) => nil
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


(fact "list-of"
  (let [ls (list-of Str)
        lm (list-of Money)]
    (fact "parse"
      (parse ls []) => []
      (parse ls nil) => []
      (parse ls "abc") => ["abc"]
      (parse ls ["ab" "cd"]) => ["ab" "cd"]
      (fact "with conversion"
        (parse lm ["1" "2"]) => [100 200]
        (parse lm ["asdf" "1"]) => [nil 100]))

    (fact "show"
      (show ls []) => []
      (show ls nil) => []
      (show ls "abc") => ["abc"]
      (show ls ["ab" "cd"]) => ["ab" "cd"]
      (fact "with conversion"
        (show lm [1 2]) => ["$0.01" "$0.02"]))

    (fact "validate"
      (validate ls ["ab" "cd"]) => truthy
      (validate ls []) => truthy
      (validate ls nil) => truthy
      (validate ls "abc") => truthy
      (validate ls {}) => falsey
      (validate ls [{} {}]) => falsey
      (fact "with conversion"
        (validate lm ["1" "2"]) => truthy
        (validate lm "1") => truthy
        (validate lm "asdf") => falsey
        (validate lm ["asdf" "1"]) => falsey))))



; Misc date/time helpers
(fact "hours"
  (hours 2) => (* 2 one-hour))

(fact "minutes"
  (minutes 15) => (* 15 one-minute))

(fact "just-date"
  (let [n (now)]
    (just-date (+ 1 n)) => (just-date n)))

(fact "date-range"
  (let [day (ts-from-date 2013 7 15)
        res (date-range day -1 1)]
    (map :text res) => ["Sun Jul 14" "Mon Jul 15" "Tue Jul 16"]
    (map :val res) => [(- day one-day) day (+ day one-day)]))

(fact "time-range"
  (let [res (time-range (hours 8) (hours 9) (minutes 30))]
    (map :text res) => ["08:00 AM" "08:30 AM" "09:00 AM"]
    (map :val res) => [(hours 8) (+ (hours 8) (minutes 30)) (hours 9)]))

(fact "timespan-range"
  (let [res (timespan-range (hours 2) (minutes 30))]
    (map :text res) => ["0.00" "0.50" "1.00" "1.50" "2.00"]
    (map :val res) => [0 (minutes 30) (hours 1) (minutes 90) (hours 2)]))

