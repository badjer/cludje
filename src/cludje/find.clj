(ns cludje.find)

(defn arity [f] 
  (let [m (first (.getDeclaredMethods (class f))) 
        p (.getParameterTypes m)] 
    (alength p)))

(defn- to-symbol [thing]
  (cond 
    (symbol? thing) thing
    (keyword? thing) (symbol (name thing))
    (= "" thing) nil
    :else (symbol thing)))

(defn- find-ns-var [nas thing]
  (when (and nas thing)
    (ns-resolve nas (to-symbol thing))))

(defn- qualify [nas thing]
  (let [sym-thing (to-symbol thing)
        str-thing (name sym-thing)]
    ; If the thing already contains the namespace, just return it
    (if (re-find (re-pattern (str "^" (name nas))) str-thing)
      sym-thing
      (to-symbol (str (name nas) "/" str-thing)))))

(defn find-in-ns 
  "Find the var thing in the namespace nas"
  ([nas thing]
    (when-let [vr (find-ns-var nas thing)]
      (qualify nas thing))))


