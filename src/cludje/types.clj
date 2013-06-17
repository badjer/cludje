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
