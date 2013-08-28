(ns cludje.validate
  (:use cludje.types)
  (:require [clojure.string :as s]))

(defn friendly-name 
  ([model field]
   (if-let [names (:fieldnames (meta model))]
     (get names field)
     (friendly-name field)))
  ([field] (s/capitalize (name field)))) 

(defn needs [data & kees]
  "Generate an error if any of the supplied keys is missing from data"
  (apply merge
         (for [kee kees]
           (if-not (value? (kee data)) 
             {kee (str "Please supply a value for " (friendly-name kee))}))))

(defn bad [f x]
  "Returns true only if x has a value and f also fails"
  (and (value? x) (not (validate-test f x))))

(defn no-bad [f m & kees]
  "Returns a map of errors if any of the supplied kees are bad f"
  (apply merge
         (for [kee kees]
           (if (bad f (get m kee))
             {kee (str "Can't understand " (friendly-name kee))}))))

