(ns cludje.validation)

(defn value? [x]
  "Returns true if x is truthy and not an empty string."
  (and x (not= x "")))

(defn needs [data & kees]
  "Generate an error if any of the supplied keys is missing from data"
  (apply merge
         (for [kee kees]
           (if-not (value? (kee data)) 
             {kee (str "Please supply a value")}))))

(defn email? [x]
  "Returns true if x is an email address"
  (re-matches #"(?i)[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?" x))

(defn min-length? [x len]
  "Returns true if x is greater than or equal to the given len"
  (>= (count x) len))

(defn bad [f x]
  "Returns true only if x has a value and f also fails"
  (and (value? x) (not (f x))))

(defn no-bad [f m & kees]
  "Returns a map of errors if any of the supplied kees are bad f"
  (apply merge
         (for [kee kees]
           (if (bad f (kee m))
             {kee (str "Can't understand " (name kee))}))))

