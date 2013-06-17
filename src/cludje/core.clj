(ns cludje.core
  (:require [clojure.string :as s]
            [cludje.types :as t]
            [cludje.validation]))

(defprotocol IBuildable
  (build [self m] "Make an instance from something"))
(defn make 
  "Make an instance (calls build)"
  ([ibuildable]
   (build ibuildable {}))
  ([ibuildable m]
   (build ibuildable m))
  ([ibuildable f v & kvs]
   (build ibuildable (-> (apply hash-map kvs) 
                        (assoc f v)))))

(defprotocol IValidatable
  (problems? [self m] "Get a map of problems trying to make m"))

(defn record-name [nam]
  (symbol (str (name nam) "-type")))

(defn defmodel-record [nam fields]
  (let [kees (map #(symbol (name %)) (keys fields))
        rec-name (record-name nam)]
    `(defrecord ~rec-name [~@kees])))

(defn defmodel-singleton [nam fields opts]
  (let [rec-name (record-name nam)
        constructor (symbol (str "->" rec-name))
        numkeys (count (keys fields))]
    `(def ~nam (with-meta 
                 (apply ~constructor (repeat ~numkeys nil))
                 ~opts))))
       
(defn defmodel-problems [nam]
  (let [problems-fn (symbol (str (s/lower-case nam) "-problems?"))
        rec-name (record-name nam)]
    `(extend ~rec-name
       IValidatable
       {:problems? 
        (fn [self# m#] 
          (merge
            (apply cludje.validation/needs m# (:require (meta ~nam)))
            (into {} (for [[field# typ#] (:fields (meta ~nam))]
                       (when (not (cludje.types/validate typ# (get m# field#)))
                         [field# (str "Invalid format for " field#)])))))})))


(defn defmodel-make [nam]
  (let [parse-fn (symbol (str "parse-" (s/lower-case nam)))
        rec-name (record-name nam)
        constructor (symbol (str "->" rec-name))]
    `(extend ~rec-name
       IBuildable
       {:build 
        (fn [self# m#]
          (let [parsed# 
                (into {} 
                      (for [[field# typ#] (:fields (meta ~nam))] 
                        [field# (cludje.types/parse typ# (get m# field#))]))]
            (merge
              (apply ~constructor (repeat (count (keys ~nam)) nil))
              parsed#)))})))



(defmacro defmodel [nam fields & opts]
  (let [defaults {:require (vec (keys fields)) 
                  :fields fields}
        optmap (merge defaults (apply hash-map opts))]
    `(do
       ~(defmodel-record nam fields)
       ~(defmodel-singleton nam fields optmap)
       ~(defmodel-problems nam)
       ~(defmodel-make nam)
     )))
