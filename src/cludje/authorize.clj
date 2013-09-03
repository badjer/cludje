(ns cludje.authorize
  (:use cludje.system
        cludje.util
        cludje.model))


(defrecord TestAuthorizer [allow?]
  IAuthorizer
  (can? [self context] @allow?))

(defn >TestAuthorizer 
  ([] (>TestAuthorizer true))
  ([allow?]
    (->TestAuthorizer (atom allow?))))


(defrecord AbilityAuthorizer [auth-fns]
  IAuthorizer
  (can? [self context]
    (let [reses (map #(% context) @auth-fns)]
      (first (keep identity reses)))))

(defn >AbilityAuthorizer [& auth-fns]
  (->AbilityAuthorizer (atom (into [] auth-fns))))



(defn- match-action? [context op model]
  (let [exp-action (str (name op) "-" (tablename model))
        action (name (? context :action-sym))
        model-re (re-pattern (str "-" (tablename model) "$"))
        model-match? (re-find model-re action)]
    (cond 
      (= exp-action action) true
      (and (= :* op) model-match?) true
      :else nil)))

(defn- realize-expr [context expr]
  (if (fn? expr) (expr context) expr))

(defn- match-ability? [context [op model expr]]
  (cond
    (= :anon expr) true
    (nil? (?? context :user)) false
    (match-action? context op model) (realize-expr context expr)
    :else nil))

(defn- parse-action-forms [forms]
  (apply concat
    (for [[op model expr] (partition 3 forms)]
      (if (vector? op)
        (for [o op] [o model expr])
        [[op model expr]]))))

(defn >Ability [& forms]
  (let [calls (parse-action-forms forms)]
    (fn [context]
      (first (keep identity (map (partial match-ability? context) calls))))))
