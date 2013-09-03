(ns cludje.application
  (:use cludje.system
        cludje.pipeline
        cludje.dataadapt
        cludje.session
        cludje.authenticate
        cludje.actionfind
        cludje.moldfind
        cludje.authorize
        cludje.datastore
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
   :emailer (>TestEmailer)})

; Define pipelines
(defn >api-pipeline [system]
  (>pipeline
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
        (wrap-system system))))
