(ns cludje.core
  (:require [clojure.string :as s]
            [cludje.validation]))


(defn defmodel-constructor [nam fields]
  `(defn ~nam 
     "Constructor to build new instances of ~nam"
     ([] ~fields)
     ([m#] (merge ~fields m#))
     ([f# v# & kvs#]
      (merge ~fields
        (-> (apply hash-map kvs#)
            (assoc f# v#))))))

(defn defmodel-problems [nam fields]
  (let [problems-fn (symbol (str (s/lower-case nam) "-problems?"))
        fieldnames (keys fields)]
    `(defn ~problems-fn [x#]
       (cludje.validation/needs x# ~@fieldnames))))

  
(defmacro defmodel [nam fields]
  `(do
    ~(defmodel-constructor nam fields)
    ~(defmodel-problems nam fields)
  ))
