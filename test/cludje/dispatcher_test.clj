(ns cludje.dispatcher-test
  (:use midje.sweet
        cludje.test
        cludje.core
        cludje.dispatcher))

(defaction action1 nil)
(defaction action2 nil)

(fact "dispatcher"
  (let [disp (->Dispatcher (atom {:1 action1 :2 action2}))]
    (get-action- disp {:action :1}) => #(= % action1)
    (get-action- disp {:action :2}) => #(= % action2)
    (get-action- disp {:action :3}) => nil?
    (get-action- disp {}) => nil?
    (get-action- disp nil) => nil?
    (get-action- disp {:action "1"}) => #(= % action1)
    (get-action- disp {:action "3"}) => nil?))

(fact "find-actions on a single namespace"
  (find-actions 'cludje.testcontrollers) => (has-keys :index)
  (fact "can actually execute the found action"
    ((:index find-actions 'cludje.testcontrollers) 
     nil {:a 1}) => {:a 1}))

