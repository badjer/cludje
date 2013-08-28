(ns cludje.pipeline
  (:use cludje.util
        cludje.types
        cludje.errors
        cludje.system))

(defn wrap-system [f system]
  (fn [context]
    (-> context
        (assoc :system system)
        (f))))
(defn wrap-unparsed-input [f]
  (fn [unparsed-input]
    (-> {:unparsed-input unparsed-input}
        (f))))
(defn wrap-session [f]
  (fn [context]
    (let [session-store (? context [:system :session-store])
          in-session (current-session session-store context)
          input (assoc context :session in-session)
          output (f input)
          out-session (:session output)]
      (persist-session session-store out-session output)
      output)))
(defn wrap-parsed-input [f]
  (fn [context]
    (let [adapter (? context [:system :data-adapter])
          unparsed-input (? context :unparsed-input)
          parsed (parse-input adapter unparsed-input)]
      (-> context
          (assoc :parsed-input parsed)
          (f)))))
(defn wrap-authenticate [f]
  (fn [context]
    (let [authenticator (? context [:system :authenticator])
          user (current-user authenticator context)]
      (-> context
          (assoc :user user)
          (f)))))
(defn wrap-action [f]
  (fn [context]
    (let [finder (? context [:system :action-finder])
          action (find-action finder context)]
      (-> context
          (assoc :action-sym action)
          (f)))))
(defn wrap-input-mold [f]
  (fn [context]
    (let [moldstore (? context [:system :mold-store])
          input-mold (get-mold moldstore context)]
      (-> context
          (assoc :input-mold input-mold)
          (f)))))
(defn wrap-input [f]
  (fn [context]
    (let [input-mold (? context :input-mold)
          parsed-input (? context :parsed-input)
          molded-input (parse input-mold parsed-input)]
      (-> context
          (assoc :input molded-input)
          (f)))))
(defn wrap-authorize [f]
  (fn [context]
    (let [system (? context :system)
          authorizer (? system :authorizer)
          action-sym (? context :action-sym)
          user (? context :user)
          input (? context :input)
          ok? (allowed? authorizer system action-sym user input)]
      (if-not ok?
        (throw-unauthorized)
        (f context)))))
(defn wrap-output [f]
  (fn [context]
    (let [action-sym (? context :action-sym)
          action (resolve action-sym)
          output (action context)]
      (-> context
          (assoc :output output)
          (f)))))
(defn wrap-output-mold [f]
  (fn [context]
    (let [done-context (f context)
          moldstore (? done-context [:system :mold-store])
          output-mold (get-mold moldstore done-context)]
      (assoc done-context :output-mold output-mold))))
(defn wrap-molded-output [f]
  (fn [context]
    (let [done-context (f context)
          output-mold (? done-context :output-mold)
          output (? done-context :output)
          molded (show output-mold output)]
      (assoc done-context :molded-output molded))))
(defn wrap-rendered-output [f]
  (fn [context]
    (let [done-context (f context)
          data-adapter (? done-context [:system :data-adapter])
          molded-output (? done-context :molded-output)
          rendered (render-output data-adapter molded-output)]
      (assoc done-context :rendered-output rendered))))

