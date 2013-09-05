(ns cludje.find
  (:use cludje.util))

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


(defn find-in-nses [namespaces nam]
  (keep identity (map #(find-in-ns % nam) namespaces)))

(defn search-in-nses [namespaces names]
  (keep identity (flatten (map (partial find-in-nses namespaces) names))))
