(ns cludje.core
  (:require [clojure.string :as s]
            [cludje.types :as t]
            [cludje.system :as sys]
            [cludje.validation]))

(defprotocol IBuildable
  (build [self m] "Make an instance from something"))
(defn make 
  "Make an instance (calls build)"
  ([ibuildable]
   (build ibuildable nil))
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

(defn get-problems [model-meta input]
  (merge
    (apply cludje.validation/needs input (:require model-meta))
    (into {} (for [[field typ] (:fields model-meta)]
               (when-not (cludje.types/validate typ (get input field))
                 [field (str "Invalid format for " field)])))))


(defn defmodel-problems [nam]
  (let [rec-name (record-name nam)]
    `(extend ~rec-name
       IValidatable
       {:problems? 
        (fn [self# m#] 
          (let [p# (get-problems (meta ~nam) m#)]
            (if-not (empty? p#)
              p#)))})))

(defn defmodel-make [nam]
  (let [rec-name (record-name nam)
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
  (let [table (s/lower-case (name nam))
        kee (keyword (str table "_id"))
        defaults {:require (vec (keys fields)) 
                  :fields (assoc fields kee cludje.types/Str)
                  :table table
                  :key kee}
        optmap (merge defaults (apply hash-map opts))]
    `(do
       ~(defmodel-record nam fields)
       ~(defmodel-singleton nam fields optmap)
       ~(defmodel-problems nam)
       ~(defmodel-make nam)
       )))

(defn- table-name [model]
  (:table (meta model)))

(defn- key-name [model]
  (:key (meta model)))

(defn fetch [db model kee]
  (let [tbl (table-name model)]
    (sys/fetch- db tbl kee)))

(defn query [db model params]
  (let [tbl (table-name model)]
    (sys/query- db tbl params)))

(defn write [db model kee data]
  (let [tbl (table-name model)]
    (sys/write- db tbl kee data)))

(defn delete [db model kee]
  (let [tbl (table-name model)]
    (sys/delete- db tbl kee)))

(defn- throw-problems 
  ([]
   (throw-problems {}))
  ([problems]
  (throw (with-meta (Exception. "") {:problems problems}))))

(defn get-key [model m]
  (get m (key-name model) nil))

(defn save [db model m]
  (if-let [probs (problems? model m)]
    (throw-problems probs)
    (let [parsed (make model m)
          kee (get-key model parsed)]
      (write db model kee parsed))))


;(defmacro definteraction [nam & forms]
  ;`(defn ~nam [sys request]
     ;(let [save (partial cludje.core/save (:db sys))]
       ;~@forms)))
