(ns cludje.application
  (:use cludje.system
        cludje.util
        cludje.pipeline
        cludje.dataadapt
        cludje.session
        cludje.authenticate
        cludje.actionfind
        cludje.moldfind
        cludje.authorize
        cludje.datastore
        cludje.serve
        cludje.email
        cludje.log))


(defn >test-system [{:keys [action-namespaces mold-namespaces]}]
  {:data-adapter (>TestDataAdapter)
   :session-store (>TestSessionStore)
   :authenticator (>TestAuthenticator)
   :action-finder (apply >NSActionFinder action-namespaces)
   :mold-finder (apply >NSMoldFinder mold-namespaces)
   :authorizer (>TestAuthorizer)
   :logger (>TestLogger)
   :data-store (>TestDatastore)
   :emailer (>TestEmailer)
   })


(defn with-web [system]
  (-> system
      (assoc :server (>JettyServer))
      (assoc :data-adapter (>WebDataAdapter))
      (assoc :port 8888)))


; Define pipelines
(defn >test-pipeline [system]
  (-> identity
      (wrap-output)
      (wrap-input)
      (wrap-input-mold)
      (wrap-action)
      (wrap-session)
      (wrap-parsed-input)
      (wrap-system system)
      (wrap-context)
      (unwrap-context :output)))


(defn >api-pipeline [system]
  (-> identity
      (wrap-output)
      (wrap-output-mold)
      (wrap-molded-output)
      (wrap-rendered-output)
      (wrap-authorize)
      (wrap-input)
      (wrap-input-mold)
      (wrap-action)
      (wrap-authenticate)
      (wrap-session)
      (wrap-parsed-input)
      (wrap-session)
      (wrap-system system)
      (wrap-context)
      (unwrap-context :rendered-output)))

; System functions
(defn start-system [system]
  (let [server (? system :server)
        port (? system :port)
        handler (>api-pipeline system)]
    (start server port handler)
    system))

(defn stop-system [system]
  (let [server (? system :server)]
    (stop server)
    system))

