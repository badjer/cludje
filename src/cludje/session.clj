(ns cludje.session
  (:use cludje.system
        cludje.util))

(defrecord TestSessionStore [contents]
  ISessionStore
  (add-session [store context]
    (assoc context :session @contents))
  (persist-session [store context]
    (when-let [session (?? context :session)]
      (reset! contents session)
      context)))

(defn >TestSessionStore 
  ([] (>TestSessionStore {}))
  ([contents]
    (->TestSessionStore (atom contents))))

(defrecord RingSessionStore []
  ISessionStore
  (add-session [store context]
    (let [ring-request (?! context :ring-request)]
      (println "RING-REQUEST: " ring-request)
      (-> context 
          (assoc :session (?? ring-request :session))
          (assoc :session/key (?? ring-request :session/key)))))
  (persist-session [store context]
    ; Do nothing, because we assume that the data-adapter handles it
    context))



(defn >RingSessionStore []
  (->RingSessionStore))
