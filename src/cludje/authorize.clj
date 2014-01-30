(ns cludje.authorize
  (:use cludje.system
        cludje.util
        cludje.model))


(defrecord TestAuthorizer [allow?]
  IAuthorizer
  (can? [self request] @allow?))

(defn >TestAuthorizer 
  ([] (>TestAuthorizer true))
  ([allow?]
    (->TestAuthorizer (atom allow?))))


(defrecord AbilityAuthorizer [auth-fns]
  IAuthorizer
  (can? [self request]
    (let [reses (map #(% request) @auth-fns)]
      (first (keep identity reses)))))

(defn >AbilityAuthorizer [& auth-fns]
  (->AbilityAuthorizer (atom (into [] auth-fns))))



(defn- match-action? [request op model]
  (let [exp-action (str (name op) "-" (tablename model))
        action (name (? request [:input :_action]))
        model-re (re-pattern (str "-" (tablename model) "$"))
        model-match? (re-find model-re action)]
    (cond 
      (= exp-action action) true
      (and (= :* op) model-match?) true
      :else nil)))

(defn- realize-expr [request expr]
  (if (fn? expr) (expr request) expr))

(defn- match-ability? [request [op model expr]]
  (cond
    (= :anon expr) true
    (nil? (?? request :user)) false
    (match-action? request op model) (realize-expr request expr)
    :else nil))

(defn- parse-action-forms [forms]
  (apply concat
    (for [[op model expr] (partition 3 forms)]
      (if (vector? op)
        (for [o op] [o model expr])
        [[op model expr]]))))

(defn >Ability [& forms]
  (let [calls (parse-action-forms forms)]
    (fn [request]
      (first (keep identity (map (partial match-ability? request) calls))))))
