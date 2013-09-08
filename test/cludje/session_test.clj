(ns cludje.session-test
  (:use midje.sweet
        cludje.system
        cludje.test
        cludje.session))

(def session {:a 1})
(def good-context {:raw-input {:session session}})
(def bad-context {:raw-input {}})

(facts ">RingSessionStore"
  (let [ss (>RingSessionStore)]
    (fact "current-session"
      (fact "finds context"
        (current-session ss good-context) => session)
      (fact "throws if session missing from input"
        (current-session ss bad-context) => (throws-error)))
    (fact "persist-session"
      (future-fact "writes back"))))
