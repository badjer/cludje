(ns cludje.types
  (:require [clojure.string :as s]))

(defprotocol IParseable
  (parse [self txt]))

(defprotocol IShowable
  (show [self x]))

(defprotocol IValidateable
  (problems? [self m] "Get the problems trying to make m"))

(defn validate [ivalidateable x]
  "Determine if x is a valid value of the supplied type"
  (not (problems? ivalidateable x)))

(def Str 
  (reify 
    IParseable 
    (parse [self txt] (when txt (str txt)))
    IShowable
    (show [self x] x) 
    IValidateable
    ; Never any problems with str
    (problems? [self txt])))

(def Email
  (reify 
    IParseable 
    (parse [self txt] (when txt (str txt)))
    IShowable
    (show [self x] x) 
    IValidateable
    (problems? [self txt]
      (cond
        (nil? txt) nil
        (empty? (str txt)) nil 
        (not (re-find #"(?i)[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?" txt))
        "Not a valid email"))))

(def Password
  (reify 
    IParseable
    (parse [self txt] (when txt (str txt)))
    IShowable
    (show [self x] "")
    IValidateable
    (problems? [self txt]
      (cond
        (empty? txt) nil
        (> 2 (.length txt)) "Not long enough"))))

(defn- to-int [x]
  (if-not (nil? x)
    (let [t (type x)]
      (cond
        (= t java.lang.Long) x
        (= t java.lang.Integer) x
        (= t BigDecimal) (.intValue x)
        :else (Long. x)))))

(defn- to-decimal [x]
  (if (or (nil? x) (= BigDecimal (type x)))
    x
    (BigDecimal. x)))

(def Int
  (reify 
    IParseable
    (parse [self txt] (when-not (empty? (str txt)) (to-int txt)))
    IShowable
    (show [self x] (str x))
    IValidateable
    (problems? [self txt]
      (when (not (re-find #"^\d*$" (str txt)))
        "Not a number"))))


(defn to-2-digit 
  ([x] (to-2-digit x true))
  ([x with-sign?]
   (when x
     (let [res (if (and (> x -10) (< x 10)) (str "0" x) (str x))]
       (if with-sign?
         res
         (s/replace res "-" ""))))))


(defn- money-str
  "Return a user-friendly string representation of a dollar amount.
  Assumes that the amounts input are in cents.
  If print-cents? is true, cents will always print. If false, it will
  never print. If nil, cents will only print if they are not 0"
  ([n]
   (money-str n nil true))
  ([n print-cents?]
   (money-str n print-cents? true))
  ([n print-cents? include-dollar-sign?]
   {:pre [(not= (type n) java.lang.Double)]}
   (if-not (nil? n)
     (let [decversion (to-decimal n)
           dollars (quot decversion 100)
           cents (rem decversion 100)
           cents-str (str "." (to-2-digit cents false))
           show-cents? (or print-cents? 
                           (and (nil? print-cents?) (not (zero? cents))))
           dollar-str (s/replace (str dollars) "-" "")
           sign-str (if (neg? decversion) "-" "")]
       (str sign-str 
            (when include-dollar-sign? "$")
            dollar-str 
            (if show-cents? cents-str ""))))))

(def money-regex #"^ *\$? *(\-?) *(\d+\.?\d*) *$")

(def Money
  (reify 
    IParseable
    (parse [self txt]
      (cond
        (number? txt) txt
        (empty? (str txt)) nil
        :else
          (if-let [match (first (re-seq money-regex (str txt)))]
            (let [[_ sign n] match
                  numstr (str sign n)
                  decversion (to-decimal numstr)]
              (to-int (* 100 decversion))))))
    IShowable
    (show [self x] (money-str x))
    IValidateable
    (problems? [self txt]
      (cond
        (empty? (str txt)) nil 
        (not (re-find money-regex (str txt)))
        "Invalid money amount"))))

(def Bool
  (reify 
    IParseable
    (parse [self txt]
      (cond
        (empty? txt) nil
        1 true
        true true
        false false
        0 false
        (re-find #"^[Yy](es)?$" (str txt)) true
        (re-find #"^[tT](rue)?$" (str txt)) true
        (re-find #"^[Nn](o)?$" (str txt)) false
        (re-find #"^[fF](alse)$" (str txt)) false
        :else nil))
    IShowable
    (show [self x] (if x "yes" "no"))
    IValidateable
    (problems? [self txt]
      (cond
        (empty? (str txt)) nil
        (nil? (parse Bool txt))
        "Not a true/false value"))))


(def one-minute (* 60 1000))
(def one-hour (* 60 one-minute))
(def one-day (* 24 one-hour))

(defn- new-date [ts]
  (when ts
    (let [dt (java.util.Calendar/getInstance (java.util.SimpleTimeZone. 0 ""))]
      (.setTimeInMillis dt ts)
      dt)))

(defn- ts-from-date [y m d]
  (let [dt (new-date 0)]
    (.set dt (to-int y) (- (to-int m) 1) (to-int d))
    (.getTimeInMillis dt)))

(defn- match-time [s]
  (if-let [time-match (first (re-seq #"^ *(\d{1,2}):(\d\d) (AM|PM) *$" (str s)))]
    (let [[_ h m half] time-match
          pm? (= "PM" half)
          int-h (to-int h)
          adj-h (cond
                  (and pm? (= 12 int-h)) 12
                  (and (not pm?) (= 12 int-h)) 0
                  pm? (+ int-h 12)
                  :else int-h)]
      (+ (* one-minute (to-int m)) (* one-hour adj-h)))))

(defn- match-duration [s]
  (if-let [duration-match (first (re-seq #"^ *(\d{1,2}):(\d\d) *$" (str s)))]
    (let [[_ h m] duration-match]
      (+ (* one-minute (to-int m)) (* one-hour (to-int h))))))

(defn- match-duration-dec [s]
  (if-let [[_ n] (re-find #"^ *([\d\.]+) *$" (str s))] 
    (let [nu (to-decimal n)]
      (if (>= nu one-minute)
        nil ; If it's a number with no decimal, but 60000 or more
        (-> nu
            (* one-hour)
            (to-int))))))

(defn- match-date [s]
  (if-let [date-match (first (re-seq #"^ *(\d{4})-(\d{2})-(\d{2}) *$" (str s)))]
    (let [[_ y m d] date-match]
      (ts-from-date y m d))))

(defn- match-number [x]
  (if (number? x) 
    x
    (if-let [num-match (first (re-seq #"^\d+$" (str x)))]
      (to-int x))))

(defn- to-time [s]
  (let [time-match (match-time s)
        duration-match (match-duration s)
        duration-dec-match (match-duration-dec s)
        date-match (match-date s)
        number-match (match-number s)]
    (first (filter identity 
                   [time-match duration-match duration-dec-match
                    date-match number-match]))))

(defn- year [ts]
  (when-let [d (new-date ts)]
    (.get d java.util.Calendar/YEAR)))
(defn- month [ts]
  (when-let [d (new-date ts)]
    (+ 1 (.get d java.util.Calendar/MONTH))))
(defn- day [ts]
  (when-let [d (new-date ts)]
    (.get d java.util.Calendar/DAY_OF_MONTH)))
(defn- hour [ts]
  (when-let [d (new-date ts)]
    (.get d java.util.Calendar/HOUR_OF_DAY)))
(defn- minute [ts]
  (when-let [d (new-date ts)]
    (.get d java.util.Calendar/MINUTE)))
(defn- day-of-week [ts]
  (when-let [d (new-date ts)]
    (.get d java.util.Calendar/DAY_OF_WEEK)))

(defn- now [] 
  (let [tz (java.util.TimeZone/getDefault)
        offset (.getRawOffset tz)
        dst (.getDSTSavings tz)
        ts (System/currentTimeMillis)]
    (+ ts offset dst)))



(def months ["" "Jan" "Feb" "Mar" "Apr" "May" "Jun"
             "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"])

(def days-of-week ["" "Sun" "Mon" "Tue" "Wed" "Thu" "Fri" "Sat"])

(defn- date-str [ts]
  (when ts
    (let [y (year ts)
          cur-year (year (now))]
      (if (not= y cur-year)
        (str (get months (month ts)) " " (day ts) ", " (year ts))
        (str (get days-of-week (day-of-week ts)) " "
             (get months (month ts)) " " (day ts))))))

(def Date
  (reify 
    IParseable
    (parse [self txt] (to-time txt))
    IShowable
    (show [self x] (date-str x))
    IValidateable
    (problems? [self txt]
      (cond
        (nil? txt) nil
        (= txt "") nil
        (nil? (parse Date txt))
        "Not a date"))))

(defn- time-str [ts]
  (let [h (hour ts)
        m (minute ts)
        adj-h (cond (= 0 h) 12
                    (> h 12) (- h 12)
                    :else h)
        pm? (< 11 h)]
    (str (to-2-digit adj-h) ":" (to-2-digit m) " " (if pm? "PM" "AM"))))

(def Time
  (reify 
    IParseable
    (parse [self txt] (to-time txt))
    IShowable
    (show [self x] (time-str x))
    IValidateable
    (problems? [self txt]
      (cond
        (nil? txt) nil
        (= "" txt) nil
        (nil? (parse Time txt))
        "Not valid time"))))

(defn percentage [total part] 
  (if (zero? total) 
    0 
    (quot (* 100 part) total)))

(defn- duration-str [ts]
  (let [d (- (day ts) 1)
        h (hour ts)
        hours (+ h (* 24 d))
        m (minute ts)
        m-dec (percentage 60 m)]
    (str hours "." (to-2-digit m-dec))))

(def Timespan
  (reify 
    IParseable
    (parse [self txt] (to-time txt))
    IShowable
    (show [self x] (duration-str x))
    IValidateable
    (problems? [self txt]
      (cond
        (nil? txt) nil
        (= "" txt) nil
        (nil? (parse Timespan txt))
        "Not valid timespan"))))

(def DateTime
  (reify 
    IParseable
    (parse [self txt] (to-time txt))
    IShowable
    (show [self x] (time-str x))
    IValidateable
    (problems? [self txt]
      (cond
        (nil? txt) nil
        (= "" txt) nil
        (nil? (parse DateTime txt))
        "Not valid date/time"))))

