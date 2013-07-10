(ns cludje-test
  (:use midje.sweet)
  (:require [cludje]))

; We just need to make sure some things are exposed
; in the cludje namespace

(facts "Expose API"
  (fact "defmodel"
    (var cludje/defmodel) =not=> nil?)
  (fact "defaction"
    (var cludje/defaction) =not=> nil?))
