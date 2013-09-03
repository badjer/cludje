(ns cludje.actionfind-test
  (:use midje.sweet
        cludje.test
        cludje.system
        cludje.actionfind)
  (:require [cludje.altnamespace :as ans]))

(defn >input [action-str]
  {:parsed-input {:_action action-str}})

(defn an-action [context])
(defn not-an-action [a b c])

(fact "looks-like-action?"
  (looks-like-action? `an-action) => truthy
  (looks-like-action? `not-an-action) => falsey)

(fact "NSActionFinder"
  (let [af (>NSActionFinder 'cludje.actionfind-test 'cludje.altnamespace)]
    (fact ">NSActionFinder return IAction"
      (satisfies? IActionFinder af) => true)
    (fact "finds things"
      (find-action af (>input :an-action)) => `an-action)
    (fact "finds things in multiple namespaces"
      (find-action af (>input :altns-action)) => `ans/altns-action)
    (fact "throws exception if the thing doesn't look like an action"
      (find-action af (>input :not-an-action)) => (throws-error))))
