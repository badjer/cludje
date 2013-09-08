(ns cludje.session
  (:use cludje.system
        cludje.util))

(defrecord TestSessionStore [contents]
  ISessionStore
  (current-session [store context]
    @contents)
  (persist-session [store session context]
    (reset! contents session)))

(defn >TestSessionStore 
  ([] (>TestSessionStore {}))
  ([contents]
    (->TestSessionStore (atom contents))))

(defrecord RingSessionStore []
  ISessionStore
  (current-session [store context]
    (?! context [:raw-input :session]))
  (persist-session [store session context]
    ))

(defn >RingSessionStore []
  (->RingSessionStore))
