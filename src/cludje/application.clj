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


(defn with-web [system]
  (let [web-data-adapter (>WebDataAdapter)]
    (-> system
        (assoc :data-adapter web-data-adapter))))


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
      (wrap-system system)
      (wrap-context)
      (unwrap-context :rendered-output)))
