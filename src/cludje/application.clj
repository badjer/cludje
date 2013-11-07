(ns cludje.application
  (:use cludje.authenticate
        cludje.actionfind
        cludje.moldfind
        cludje.authorize
        cludje.datastore
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
   })
