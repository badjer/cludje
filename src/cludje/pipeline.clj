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

(defn wrap-parsed-input [f]
  (fn [context]
    (let [adapter (?! context [:system :data-adapter])
          unparsed-input (dissoc context :system)
          ;unparsed-input (?! context :raw-input)
          parsed (parse-input adapter unparsed-input)]
      (-> context
          (assoc :parsed-input parsed)
          (f)))))

(defn wrap-session [f]
  (fn [context]
    (let [session-store (?! context [:system :session-store])
          in-session (current-session session-store context)
          input (assoc context :session in-session)
          output (f input)
          out-session (:session output)]
      (persist-session session-store out-session output)
      output)))


(defn wrap-authenticate [f]
  (fn [context]
    (let [authenticator (?! context [:system :authenticator])
          user (current-user authenticator context)]
      (-> context
          (assoc :user user)
          (f)))))

(defn wrap-action [f]
  (fn [context]
    (let [finder (?! context [:system :action-finder])
          action (find-action finder context)]
      (-> context
          (assoc :action-sym action)
          (f)))))

(defn wrap-input-mold [f]
  (fn [context]
    (let [moldfinder (?! context [:system :mold-finder])
          input-mold (find-input-mold moldfinder context)]
      (-> context
          (assoc :input-mold input-mold)
          (f)))))

(defn wrap-input [f]
  (fn [context]
    (let [input-mold (?! context :input-mold)
          parsed-input (?! context :parsed-input)
          molded-input (parse input-mold parsed-input)]
      (-> context
          (assoc :input molded-input)
          (f)))))

(defn wrap-authorize [f]
  (fn [context]
    (let [authorizer (?! context [:system :authorizer])
          ok? (can? authorizer context)]
      (if-not ok?
        (throw-unauthorized)
        (f context)))))

(defn run-action [action context]
  (try
    (action context)
    (catch clojure.lang.ExceptionInfo ex
      (let [exd (ex-data ex)]
        (if (:__problems exd)
          (assoc context :output (merge (:input context) exd))
          (throw ex))))))

(defn wrap-output [f]
  (fn [context]
    (let [action-sym (?! context :action-sym)
          action (resolve action-sym)
          done-context (run-action action context)]
          ;output (run-action action context)]
      (f done-context))))
      ;(-> done-context
          ;(assoc :output output)
          ;(f)))))

(defn wrap-output-mold [f]
  (fn [context]
    (let [done-context (f context)]
      (if (:output-mold done-context)
        done-context
        (let [moldfinder (?! done-context [:system :mold-finder]) 
              output-mold (find-output-mold moldfinder done-context)] 
          (assoc done-context :output-mold output-mold))))))

(defn wrap-molded-output [f]
  (fn [context]
    (let [done-context (f context)
          output-mold (?! done-context :output-mold)
          output (?! done-context :output)
          molded (show output-mold output)]
      (assoc done-context :molded-output molded))))

(defn wrap-rendered-output [f]
  (fn [context]
    (let [done-context (f context)
          data-adapter (?! done-context [:system :data-adapter])
          rendered (render-output data-adapter done-context)]
      (update-in done-context [:rendered-output] merge rendered))))


; Pipeline constructor functions
(defn wrap-context [f]
  (fn [raw-input]
    (f raw-input)))
    ;(f {:raw-input raw-input})))

(defn unwrap-context [f selector]
  (fn [context]
    (let [done-context (f context)
          res (selector done-context)]
      (if-not (empty? res)
        res
        (throw-error {:context (dissoc done-context :system)})))))


(defn >pipeline [f]
  "Loads the input into a context, and extracts the output from the context"
  (-> f
      (wrap-context)
      (unwrap-context :rendered-output)))
