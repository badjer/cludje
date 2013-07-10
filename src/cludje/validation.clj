(ns cludje.validation)

(defprotocol IValidateable
  (problems? [self m] "Get the problems trying to make m"))

(defn validate [ivalidateable x]
  "Determine if x is a valid value of the supplied type"
  (not (problems? ivalidateable x)))

(defn validate-test [pred-or-ivalidateable x]
  (if (extends? IValidateable (type pred-or-ivalidateable))
    (validate pred-or-ivalidateable x)
    (pred-or-ivalidateable x)))

(defn value? [x]
  "Returns true if x is truthy and not an empty string."
  (and x (not= x "")))

(defn needs [data & kees]
  "Generate an error if any of the supplied keys is missing from data"
  (apply merge
         (for [kee kees]
           (if-not (value? (kee data)) 
             {kee (str "Please supply a value")}))))

(defn bad [f x]
  "Returns true only if x has a value and f also fails"
  (and (value? x) (not (validate-test f x))))

(defn no-bad [f m & kees]
  "Returns a map of errors if any of the supplied kees are bad f"
  (apply merge
         (for [kee kees]
           (if (bad f (get m kee))
             {kee (str "Can't understand " (name kee))}))))

