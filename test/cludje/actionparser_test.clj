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
    (get-action-key- ap nil) => nil)
  (facts "works with initial - (ie, for -system model)"
    (get-model-name- ap {:_action "-system-data"}) => "System"
    (get-action-key- ap {:_action "-system-data"}) => :data)
  (facts "works with single-word input - doesn't explode, at least"
    (get-model-name- ap {:_action "default"}) => nil
    (get-action-key- ap {:_action "default"}) => :default
    (get-model-name- ap {:_action "-default"}) => ""
    (get-action-key- ap {:_action "-default"}) => :default))
