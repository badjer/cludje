(ns cludje.core
  (:require [clojure.string :as s]
            [cludje.types :as t]
            [cludje.validation]))

(defprotocol IMakeable
  (make [self m] "Make an instance from something"))
(defprotocol IValidatable
  (get-problems? [self m] "Get a map of problems trying to make m"))

(defn defmodel-record [nam fields]
  (let [kees (map #(symbol (name %)) (keys fields))
        rec-name (symbol (str (name nam) "-model"))]
    `(defrecord ~rec-name [~@kees])))

(defn defmodel-constructor [nam fields]
  `(defn ~nam 
     "Constructor to build new instances of ~nam"
     ([] ~fields)
     ([m#] (merge ~fields m#))
     ([f# v# & kvs#]
      (merge ~fields
             (-> (apply hash-map kvs#)
                 (assoc f# v#))))))

(defn defmodel-problems [nam]
  (let [problems-fn (symbol (str (s/lower-case nam) "-problems?"))]
    `(defn ~problems-fn [x#]
       (apply cludje.validation/needs x# (keys (~nam))))))

(defn defmodel-parse [nam]
  (let [parse-fn (symbol (str "parse-" (s/lower-case nam)))]
    `(defn ~parse-fn [x#]
       (reduce conj {} (for [[ke# typ#] (~nam)]
                         [ke# (t/parse typ# (ke# x#))])))))

       


(defmacro defmodel [nam fields]
  `(do
     ~(defmodel-record nam fields)
     ~(defmodel-constructor nam fields)
     ~(defmodel-problems nam)
     ~(defmodel-parse nam)
     ))
