(ns cludje.test
  (:use midje.sweet))

(defn has-keys [& kees]
  "A midje checker that returns truthy if the thing
  being tested contains all the specified keys"
  (contains (zipmap kees (repeat anything))))

(defn just-keys [& kees]
  "A midje checker that returns truthy if the thing
  being tested contains only the specified keys"
  (just (zipmap kees (repeat anything))))

(defn has-problems? [x]
  "A midje checker that returns true if the thing
  being tested is a cludje response that has problems"
  (contains? x :problems))

(defn has-problems [& kees]
  "A midje checker that returns true if the thing
  being tested is a cludje response that has problems
  that contain the specified keys"
  (fn [x] 
    (and (contains? x :problems)
         ((apply has-keys kees) (:problems x)))))

