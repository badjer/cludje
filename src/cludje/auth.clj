(ns cludje.auth
  (:require [clojure.string :as s])
  (:use cludje.core))

(defrecord Auth [authfn]
  IAuth
  (authorize- [self action model user input] 
    (@authfn action model user input)))

(defn mock-auth-fn [action model user input]
  (when user true))

(defn make-auth-fn [& authfns]
  (fn [action model user input] 
    (some identity (map #(% action model user input) authfns))))

(defn make-auth [& authfns]
  (let [authfn (if (seq authfns)
                 (apply make-auth-fn authfns)
                 mock-auth-fn)]
    (->Auth (atom authfn))))

(defn is-ability? [vr]
  (let [m (meta vr)]
    (get m :ability)))

(defn find-abilities [root-ns]
  ; Find all the defabilities under the specified namespace
  ; (including all namespaces that start with it)
  ; and build them into an auth fn
  (when root-ns
    (let [ns-str (s/replace (name root-ns) #"^[^\.]+\." "")]
      (load ns-str)
      (into [] (for [f (vals (ns-publics root-ns)) :when (is-ability? f)]
                 f)))))
