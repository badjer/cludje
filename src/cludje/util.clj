(ns cludje.util
  (:use cludje.errors))

(defn map-vals [m f]
  (into {} (for [[k v] m] [k (f v)])))

(defn arity [f] 
  (let [m (first (.getDeclaredMethods (class f))) 
        p (.getParameterTypes m)] 
    (alength p)))

(defn to-symbol [thing]
  (cond 
    (symbol? thing) thing
    (keyword? thing) (symbol (name thing))
    (= "" thing) nil
    :else (symbol thing)))

(defn realize [x]
  "Get the value of x. If it is a fn, invoke it. Else just return"
  (cond 
    (and (fn? x) (= 0 (arity x))) (x)
    :else x))

(declare ?-)

(defn- ?-val [ex-fn input kee]
  (if-let [res (get input kee)]
    res
    (if ex-fn
      (ex-fn {kee " is required but was not provided"}) )))

(defn- ?-vec [ex-fn input [k & ks]]
  (if (empty? ks)
    (?-val ex-fn input k)
    (?- ex-fn (?- ex-fn input k) (vec ks))))

(defn- ?- [ex-fn input kee]
  (if-not (map? input) 
    (when ex-fn
      (throw-error {:input (str "? must be passed a map as input, but was " input)}))
    (cond 
      (vector? kee) (?-vec ex-fn input kee) 
      :else (?-val ex-fn input kee))))

(defn ? 
  "Returns kee from input, throwing problems if it's not found.
  The exception will contain problem data, so that it will be
  caught by the handler if defaction, meaning errors won't 
  crash an action.
  kee can be a vector for nested maps"
  ([input kee]
   (?- throw-problems input kee)))


(defn ?!
  "Exactly like ?, but throws a system error instead of a
  problems exception. Use this to query for things that mean
  a system-level error has happened; use ? to indicate that
  it's a user-input problem"
  ([input kee]
   (?- throw-error input kee)))

(defn ?? 
  "Returns kee from input, but does NOT throw an exception if it's
  not found (unlike ?) - return default (or nil) instead"
  ([input kee]
   (?? input kee nil))
  ([input kee default]
   (or (?- nil input kee) default)))

(defn &? [input & kees]
  "Returns the value of the first of kees found in input.
  If none are found, problems excption will be thrown, like ?"
   (when-not (map? input)
     (throw-error {:input (str "&? must be passed a map as input, but was " input)}))
  (when (empty? kees)
    (throw-problems {nil "Called &? with no kees"}))
  (let [vs (filter identity (map (partial ?? input) kees))]
    (if (empty? vs)
      (throw-problems (zipmap kees (repeat "At least one was expected")))
      (first vs))))

(defn with-alert [m typ text]
  (update-in m [:__alerts] conj {:text text :type typ}))

(defn with-problem [m field text]
  (update-in m [:__problems] assoc (keyword (name field)) (str text)))


