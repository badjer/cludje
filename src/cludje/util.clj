(ns cludje.util)

(defn map-vals [m f]
  (into {} (for [[k v] m] [k (f v)])))

(defn realize [x]
  "Get the value of x. If it is a fn, invoke it. Else just return"
  (cond 
    (fn? x) (x)
    :else x))

