(ns cludje.dispatcher-test
  (:use midje.sweet
        cludje.test
        cludje.core
        cludje.dispatcher))

(defaction action1 nil)
(defaction action2 nil)

(fact "dispatcher"
  (let [disp (->Dispatcher (atom {:1 action1 :2 action2}))]
    (get-action- disp {:_action :1}) => #(= % action1)
    (get-action- disp {:_action :2}) => #(= % action2)
    (get-action- disp {:_action :3}) => nil?
    (get-action- disp {}) => nil?
    (get-action- disp nil) => nil?
    (get-action- disp {:_action "1"}) => #(= % action1)
    (get-action- disp {:_action "3"}) => nil?))

(fact "find-actions on a single namespace"
  (find-actions 'cludje.testcontrollers) => (has-keys :index)
  (fact "can actually execute the found action"
    ((:index find-actions 'cludje.testcontrollers)
     nil {:a 1}) => {:a 1}))

(fact "find-actions only finds actions, not other fns"
  (find-actions 'cludje.testcontrollers) => (just-keys :index))

(future-fact "find-actions finds things in sub-namespaces"
  (let [dis (find-actons 'cludje.testcontrollers)]
    ; This isn't super urgent - do it later
    dis => (has-keys :index) ; in the root ns, controllers are bare
    ; Loading from cludje.testcontrollers.guest should result
    ; in routes that have their name as guest#fn-name
    dis => (has-keys :guest#new-guest) ))

(fact "make-dispactcher"
  (let [disp (make-dispatcher 'cludje.testcontrollers)]
    (get-action- disp {:_action :index}) =not=> nil?
    ; Try executing the action to make sure we actually got it
    ((get-action- disp {:_action :index}) nil {:a 1}) => {:a 1}))
