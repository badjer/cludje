(ns cludje.pipeline-test
  (:use midje.sweet
        cludje.errors
        cludje.test
        cludje.system
        cludje.authenticate
        cludje.actionfind
        cludje.authorize
        cludje.moldfind
        cludje.types
        cludje.mold
        cludje.pipeline))


(def raw-input {:price "$1.23"})
(def raw-request {:params raw-input})

(fact "add-system"
  (let [sys {:a 1}
        handler (add-system identity sys)
        request raw-request]
    (fact "adds :system"
      (handler request) => (contains {:system sys}))
    (fact "returns original data"
      (handler request) => (contains request))))

(def user {:name "A"})
(def authenticator (>TestAuthenticator user))

(fact "add-authenticate"
  (let [sys {:authenticator authenticator}
        handler (add-authenticate identity)
        request (assoc raw-request :system sys)]
    (fact "adds :user"
      (handler request) => (contains {:user user}))
    (fact "requires system/authenticator"
      (fact "no system"
        (handler {}) => (throws-error))
      (fact "no authenticator"
        (handler raw-request) => (throws-error)))
    (fact "returns original data"
      (handler request) => (contains request))))


(def output {:price 987})
(defn action [request] (assoc request :output output))
(defn just-output-action [request] output)
(def action-finder (>SingleActionFinder `action))

(fact "add-action"
  (let [sys {:action-finder action-finder}
        handler (add-action identity)
        request (assoc raw-request :system sys)]
    (fact "adds :action"
      (handler request) => (contains {:action-sym `action}))
    (fact "requires system/action-finder"
      (fact "no system"
        (handler {}) => (throws-error))
      (fact "no action-finder"
        (handler raw-request) => (throws-error)))
    (fact "returns original data"
      (handler request) => (contains request))))

(def action-request (assoc raw-request :user user :action-sym `action))
(def mold (>Mold {:price Money} {}))
(def moldfinder (>SingleMoldFinder mold))

(def bar (>Mold {:name Str} {}))

(fact "add-input-mold"
  (let [sys {:mold-finder moldfinder}
        handler (add-input-mold identity)
        request (assoc action-request :system sys)]
    (fact "adds :input-mold"
      (:input-mold (handler request)) => mold)
    (fact "requires system/mold-finder"
      (fact "no system"
        (handler raw-request) => (throws-error))
      (fact "no mold-finder"
        (handler (assoc-in request [:system :mold-finder] nil)) => (throws-error)))
    (fact "returns original data"
      (handler request) => (contains request))))

(def input-mold-request (assoc action-request :input-mold mold))
(def input {:price 123})

(fact "add-input"
  (let [handler (add-input identity)
        request input-mold-request]
    (fact "adds :input"
      (handler request) => (contains {:input input}))
    (fact "requires params"
      (handler (dissoc request :params)) => (throws-error))
    (fact "requires input-mold"
      (handler (dissoc request :input-mold)) => (throws-error))))

(def authorizor (>TestAuthorizer true))
(def input-request
  (assoc action-request :input input))


(fact "authorize"
  (let [sys {:authorizer authorizor}
        handler (authorize identity)
        request (assoc input-request :system sys)]
    (fact "requires system/authorizer"
      (fact "no system"
        (handler {}) => (throws-error))
      (fact "no authorizer"
        (handler raw-request) => (throws-error)))
    (fact "does nothing if authorized"
      (handler request) => request)
    (fact "throws exception if unauthorized"
      (let [unauth (>TestAuthorizer false)
            unauth-sys {:authorizer unauth}
            request (assoc input-request :system unauth-sys)]
        (handler request) => (throws-403)))
    (fact "returns original data"
      (handler request) => (contains request))))


(defn problem-action [request] (throw-problems {:name "bad"}))
(defn error-action [request] (throw-error))

(fact "run-action"
  (fact "when action returns response map"
    (let [response (run-action action {:system {}})]
      (fact "sets the output"
        response => (contains {:output output}))
      (fact "returns the request map"
        response => (contains {:system {}}))))
  (fact "when action only returns output"
    (let [response (run-action just-output-action {:system {}})]
      (fact "sets the output"
        response => (contains {:output output}))
      (fact "returns the request map"
        response => (contains {:system {}}))))
  (fact "catch problems and merge them into the response map"
    (run-action problem-action {}) => {:output {:__problems {:name "bad"}}})
  (fact "any non-problem exception is propagated"
    (run-action error-action {}) => (throws)))






(fact "add-output" 
  (let [handler (add-output identity)
        sys {}
        request (assoc input-request :system sys)]
    (fact "adds :output"
      (handler request) => (contains {:output anything}))
    (fact "requires action-sym"
      (handler (dissoc request :action-sym)) => (throws-error))
    (fact "calls action"
      (handler request) => (contains {:output output}))
    (fact "returns original data"
      (handler request) => (contains request))
    (let [bad-request (assoc request :action-sym `problem-action)]
      (fact "has __problems if problems thrown"
        (:output (handler bad-request)) => (contains {:__problems {:name "bad"}}))
      (fact "has original input if problems thrown"
        (:output (handler bad-request)) => (contains input))
      (fact "throws if non-problem exception"
        (let [err-request (assoc request :action-sym `error-action)]
          (handler err-request) => (throws))))))

(fact "add-output-mold"
  (let [sys {:mold-finder moldfinder}
        handler (add-output-mold identity)
        request (assoc input-request :system sys)]
    (fact "adds :output-mold"
      (handler request) => (has-keys :output-mold))
    (fact "requires system/mold-store"
      (fact "no system"
        (handler raw-request) => (throws-error))
      (fact "no moldstore"
        (handler (assoc-in request [:system :mold-finder] nil)) => (throws-error)))
    (fact "doesn't overwrite output-mold if it's already set"
      (handler (assoc request :output-mold 1)) => (contains {:output-mold 1}))
    (fact "returns original data"
      (handler request) => (contains request))))

(def result {:price "$9.87"})
(def output-request (assoc input-request :output output :output-mold mold))

(fact "add-result"
  (let [handler (add-result identity)
        request output-request]
    (fact "turns output to result"
      (handler request) => (contains {:result result}))
    (fact "requires output-mold"
      (handler (dissoc request :output-mold)) => (throws-error))
    (fact "requires output"
      (handler (dissoc request :output)) => (throws-error))))
