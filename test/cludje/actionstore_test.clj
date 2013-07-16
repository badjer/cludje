(ns cludje.actionstore-test
  (:use midje.sweet
        cludje.core
        cludje.actionstore))

(defaction action1 nil)
(defaction action2 nil)

(fact "actionstore"
  (let [ac (->ActionStore 'cludje.actionstore-test nil)]
    (get-action- ac "action1") => #(= % action1)
    (get-action- ac "action2") => #(= % action2)
    (get-action- ac "action3") => nil
    (get-action- ac :action1) => #(= % action1)
    (get-action- ac :action2) => #(= % action2)
    (get-action- ac :action3) => nil
    (get-action- ac "") => nil
    (get-action- ac nil) => nil))
