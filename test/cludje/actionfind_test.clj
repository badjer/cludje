(ns cludje.actionfind-test
  (:use midje.sweet
        cludje.test
        cludje.system
        cludje.actionfind)
  (:require [cludje.altnamespace :as ans]))


(defn >input [action-str] {:params {:_action action-str}})

(defn an-action [request])
(defn not-an-action [a b c])

(defn test-find [f af]
  (fact "finds things"
    (find-action af (>input :an-action)) => (exactly an-action))
  (fact "finds things in multiple namespaces"
    (find-action af (>input :altns-action)) => (exactly ans/altns-action)))

(fact "NSActionFinder"
  (let [af (>NSActionFinder 'cludje.actionfind-test 'cludje.altnamespace)]
    (fact ">NSActionFinder return IAction"
      (satisfies? IActionFinder af) => true)
    (fact "find-action"
      (test-find find-action af)
      (fact "throws exception if can't find"
        (find-action af (>input :random-func)) => (throws-error))
      (fact "throws if no action supplied"
        (find-action af (>input nil)) => (throws-error))
      (fact "throws exception if the thing doesn't look like an action"
        (find-action af (>input :not-an-action)) => (throws-error)))))
