(ns cludje.session-test
  (:use midje.sweet
        cludje.system
        cludje.test
        cludje.session))

(def session {:a 1})
(def empty-context {:raw-input {}})
(def good-context {:raw-input {} :session session})
(def bad-context {:raw-input {}})
(def session-context {:session session})

(facts ">TestSessionStore"
  (let [ts (>TestSessionStore)]
    (fact "adds session"
      (add-session ts empty-context) => (contains {:session {}}))
    (fact "persists session"
      (persist-session ts good-context) => anything
      (add-session ts empty-context) => (contains {:session session})
      )))

(def empty-ring-context {:raw-input {} :ring-request {:session {}}})
(def sessionless-ring-context {:raw-input {} :ring-request {}})
(def ring-context {:raw-input {} :ring-request {:session session}})

(facts ">RingSessionStore"
  (let [ss (>RingSessionStore)]
    (fact "adds session"
      (add-session ss empty-ring-context) => (contains {:session {}}))
    (fact "reads session from ring-request session"
      (add-session ss ring-context) => (contains {:session session}))
    (fact "gets nil session if none present"
      (add-session ss sessionless-ring-context) =not=> (throws))
    (fact "throws if :ring-request missing from context"
        (add-session ss bad-context) => (throws-error))))

