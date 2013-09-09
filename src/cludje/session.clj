(ns cludje.session
  (:use cludje.system
        cludje.util))

(defrecord TestSessionStore [contents]
  ISessionStore
  (current-session [store context]
    @contents)
  (persist-session [store session context]
    (reset! contents session)
    context))

(defn >TestSessionStore 
  ([] (>TestSessionStore {}))
  ([contents]
    (->TestSessionStore (atom contents))))

(defrecord RingSessionStore []
  ISessionStore
  (current-session [store context]
    (?! context [:raw-input :session]))
  (persist-session [store session context]
    ; Do nothing, because we assume that the data-adapter handles it
    context))
    ;(update-in context [:rendered-output] merge session)))


(defn >RingSessionStore []
  (->RingSessionStore))
