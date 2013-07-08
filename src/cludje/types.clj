(ns cludje.types
  (:require [clojure.string :as s]))

(defprotocol IFieldType 
  (parse [self txt])
  (show [self x])
  (validate [self txt]))

(def Str 
  (reify IFieldType 
    (parse [self txt] (when txt (str txt)))
    (show [self x] x) 
    (validate [self txt] true)))

(def Email
  (reify IFieldType 
    (parse [self txt] (when txt (str txt)))
    (show [self x] x) 
    (validate [self txt] 
      (or (nil? txt) (or (empty? (str txt))
                         (re-find #"(?i)[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?" txt))))))

(def Password
  (reify IFieldType
    (parse [self txt] (when txt (str txt)))
    (show [self x] "")
    (validate [self txt] (or (empty? txt)
                             (< 2 (.length txt))))))

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
  (reify IFieldType
    (parse [self txt] (when-not (empty? (str txt)) (to-int txt)))
    (show [self x] (str x))
    (validate [self txt]
      (re-find #"^\d*$" (str txt)))))

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
  (reify IFieldType
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
    (show [self x] (money-str x))
    (validate [self txt] (or (empty? (str txt)) (re-find money-regex (str txt))))))

(def Bool
  (reify IFieldType
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
    (show [self x] (if x "yes" "no"))
    (validate [self txt] (or (empty? (str txt)) (not (nil? (parse Bool txt)))))))


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
  (reify IFieldType
    (parse [self txt] (to-time txt))
    (show [self x] (date-str x))
    (validate [self txt] 
      (or (nil? txt) (= "" txt) (not (nil? (parse Date txt)))))))

(defn- time-str [ts]
  (let [h (hour ts)
        m (minute ts)
        adj-h (cond (= 0 h) 12
                    (> h 12) (- h 12)
                    :else h)
        pm? (< 11 h)]
    (str (to-2-digit adj-h) ":" (to-2-digit m) " " (if pm? "PM" "AM"))))

(def Time
  (reify IFieldType
    (parse [self txt] (to-time txt))
    (show [self x] (time-str x))
    (validate [self txt] 
      (or (nil? txt) (= "" txt) (not (nil? (parse Time txt)))))))

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
  (reify IFieldType
    (parse [self txt] (to-time txt))
    (show [self x] (duration-str x))
    (validate [self txt]
      (or (nil? txt) (= "" txt) (not (nil? (parse Timespan txt)))))))

(def DateTime
  (reify IFieldType
    (parse [self txt] (to-time txt))
    (show [self x] (time-str x))
    (validate [self txt]
      (or (nil? txt) (= "" txt) (not (nil? (parse DateTime txt)))))))

