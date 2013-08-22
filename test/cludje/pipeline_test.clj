(ns cludje.pipeline-test
  (:use midje.sweet
        cludje.test
        cludje.system
        cludje.authenticate
        cludje.pipeline))

(def input {:price "$1.23"})
(def context {:rawinput input})
(defn echo [context] context)

(fact "wrap-context"
  (let [handler (wrap-context identity)]
    (fact "adds :rawinput"
      (handler input) => (contains context))))

(fact "wrap-system"
  (let [sys {:system 1}
        handler (wrap-system identity sys)]
    (fact "adds :system"
      (handler context) => (contains {:system sys}))
    (fact "returns original data"
      (handler context) => (contains context))))

(future-fact "wrap-session"
  (fact "adds :session")
  (fact "requires system/session-store")
  (fact "persists :session")
  (fact "returns original data"))

(def user {:name "A"})
(def authenticator (>TestAuthenticator user))

(fact "wrap-authenticate"
  (let [sys {:authenticator authenticator}
        handler (-> identity 
                    (wrap-authenticate)
                    (wrap-system sys))]
  (fact "adds :user"
    (handler context) => (contains {:user user}))
  (fact "requires system/authenticator"
    (fact "no system"
      ((wrap-authenticate identity) context) => throws
    (fact "no authenticator"
      ((-> identity (wrap-system {}) (wrap-authenticate)) context) => throws)))
  (fact "returns original data"
    (handler context) => (contains context))))


(def output {:count 2})
(defn action [context] (assoc context :output output))
(def action-finder (>SingleActionFinder action))

(fact "wrap-action"
  (let [sys {:action-finder action-finder}]
    (fact "adds :action")
    (fact "requires system/action-finder")
    (fact "returns original data")))

(fact "wrap-authorize"
  (fact "requires system/authorizer")
  (fact "throws exception if unauthorized")
  (fact "returns original data"))

(fact "wrap-molds"
  (fact "adds :input-mold")
  (fact "adds :output-mold")
  (fact "requires system/mold-store")
  (fact "returns original data"))

(fact "wrap-data"
  (fact "adds :input")
  (fact "adds :rawoutput")
  (fact "requires system/data-adapter")
  (fact "returns original data"))

(fact "wrap-output"
  (fact "adds :output")
  (fact "calls action")
  (fact "returns original data"))

