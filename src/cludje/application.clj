(ns cludje.application
  (:use cludje.system
        cludje.util
        cludje.pipeline
        cludje.authenticate
        cludje.actionfind
        cludje.moldfind
        cludje.authorize
        cludje.datastore
        cludje.serve
        cludje.email
        cludje.log))


(defn >test-system [{:keys [action-namespaces mold-namespaces]}]
  {:authenticator (>TestAuthenticator)
   :action-finder (apply >NSActionFinder action-namespaces)
   :mold-finder (apply >NSMoldFinder mold-namespaces)
   :authorizer (>TestAuthorizer)
   :logger (>TestLogger)
   :data-store (>TestDatastore)
   :emailer (>TestEmailer)
   :server (>TestServer)
   })


(defn with-web [system]
  (-> system
      (assoc :server (>JettyServer))
      (assoc :port 8888)))


; Define pipelines
(defn >test-pipeline [system]
  (-> identity
      (add-output)
      (add-output-mold)
      (add-result)
      (add-input)
      (add-action)
      (add-system system)))


(defn >api-pipeline [system]
  (-> identity
      (add-output)
      (add-output-mold)
      (add-result)
      (authorize)
      (add-input)
      (add-action)
      (add-authenticate)
      (add-system system)))

; System functions
(defn start-system [system]
  (let [server (?! system :server)
        handler (>api-pipeline system)]
    (start server system handler)
    system))

(defn stop-system [system]
  (let [server (?! system :server)]
    (stop server)
    system))

