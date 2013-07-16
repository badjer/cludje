(ns cludje.actionparser-test
  (:use midje.sweet
        cludje.core
        cludje.actionparser))

(let [ap (->ActionParser)]
  (facts "get-model-name"
    (get-model-name- ap {:_action "cog-add"}) => "Cog"
    (get-model-name- ap {}) => nil
    (get-model-name- ap nil) => nil)
  (facts  "get-action-key-"
    (get-action-key- ap {:_action "cog-add"}) => :add
    (get-action-key- ap {}) => nil
    (get-action-key- ap nil) => nil))
