(ns cludje.auth
  (:require [clojure.string :as s])
  (:use cludje.core))

(defn make-auth-fn [& authfns]
  (fn [system action model user input] 
    (some identity (map #(% system action model user input) authfns))))


(defn is-ability? [vr]
  (let [m (meta vr)]
    (get m :cludje-ability)))

(defn find-abilities [root-ns]
  ; Find all the defabilities under the specified namespace
  ; (including all namespaces that start with it)
  ; and build them into an auth fn
  (when root-ns
    (let [ns-str (-> (name root-ns) 
                     (s/replace #"^[^\.]+\." "")
                     (s/replace #"\." "/"))]
      (into [] (for [f (vals (ns-publics root-ns)) :when (is-ability? @f)]
                 @f)))))

(defrecord Auth [action-ns authfn]
  IAuth
  (authorize- [self system action model user input] 
    (@authfn system action model user input))
  IStartable
  (start- [self]
    (when @action-ns
      (reset! authfn (apply make-auth-fn (find-abilities @action-ns)))))
  (stop- [self]))

(defn make-auth-from-ns [{:keys [action-ns]}]
  (->Auth (atom action-ns) (atom nil)))

(defn mock-auth-fn [system action model user input]
  (when user true))

(defn make-auth [& authfns]
  (let [authfn (if (seq authfns)
                 (apply make-auth-fn authfns)
                 mock-auth-fn)]
    (->Auth (atom nil) (atom authfn))))
