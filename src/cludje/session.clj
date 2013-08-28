(ns cludje.session
  (:use cludje.system)
  (:import java.util.UUID))

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
