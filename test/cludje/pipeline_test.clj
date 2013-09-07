(ns cludje.pipeline-test
  (:use midje.sweet
        cludje.errors
        cludje.test
        cludje.system
        cludje.authenticate
        cludje.actionfind
        cludje.authorize
        cludje.moldfind
        cludje.session
        cludje.dataadapt
        cludje.types
        cludje.mold
        cludje.pipeline))


(def raw-input {:price "$1.23"})
(def raw-context {:raw-input raw-input})

(fact "wrap-system"
  (let [sys {:system 1}
        handler (wrap-system identity sys)
        context raw-context]
    (fact "adds :system"
      (handler context) => (contains {:system sys}))
    (fact "returns original data"
      (handler context) => (contains context))))


(def session-store (>TestSessionStore))

(fact "wrap-session"
  (let [add-session-handler (wrap-session #(assoc-in % [:session :a] 1))
        handler (wrap-session identity)
        sys {:session-store session-store}
        context (assoc raw-context :system sys)]
    (fact "adds :session"
      (handler context) => (has-keys :session))
    (fact "requires system/session-store"
      (fact "no system"
        (handler (dissoc context :system)) => (throws-error))
      (fact "no session-store"
        (handler (assoc context :system {})) => (throws-error)))
    (fact "persists :session"
      (add-session-handler context) => anything
      (handler context) => (contains {:session {:a 1}}))
    (fact "returns original data"
      (handler context) => (contains context))))

(def data-adapter (>TestDataAdapter))
(def parsed-input {:price "$1.23"})

(fact "wrap-parsed-input"
  (let [handler (wrap-parsed-input identity)
        sys {:data-adapter data-adapter}
        context (assoc raw-context :system sys)]
    (fact "adds :parsed-input"
      (handler context) => (contains {:parsed-input parsed-input}))
    (fact "requires system/data-adapter"
      (fact "no system"
        (handler raw-context) => (throws-error))
      (fact "no data-adapter"
        (handler (assoc context :system {})) => (throws-error)))
    (fact "requires read-input"
      (handler (dissoc context :raw-input)) => (throws-error))
    (fact "returns original data"
      (handler context) => (contains context))))

(def user {:name "A"})
(def authenticator (>TestAuthenticator user))

(fact "wrap-authenticate"
  (let [sys {:authenticator authenticator}
        handler (wrap-authenticate identity)
        context (assoc raw-context :system sys)]
    (fact "adds :user"
      (handler context) => (contains {:user user}))
    (fact "requires system/authenticator"
      (fact "no system"
        (handler {}) => (throws-error))
      (fact "no authenticator"
        (handler raw-context) => (throws-error)))
    (fact "returns original data"
      (handler context) => (contains context))))


(def output {:price 987})
(defn action [context] output)
(def action-finder (>SingleActionFinder `action))

(fact "wrap-action"
  (let [sys {:action-finder action-finder}
        handler (wrap-action identity)
        context (assoc raw-context :system sys)]
    (fact "adds :action"
      (handler context) => (contains {:action-sym `action}))
    (fact "requires system/action-finder"
      (fact "no system"
        (handler {}) => (throws-error))
      (fact "no action-finder"
        (handler raw-context) => (throws-error)))
    (fact "returns original data"
      (handler context) => (contains context))))

(def action-context (assoc raw-context :user user :action-sym `action))
(def mold (>Mold {:price Money} {}))
(def moldfinder (>SingleMoldFinder mold))

(def bar (>Mold {:name Str} {}))

(fact "wrap-input-mold"
  (let [sys {:mold-finder moldfinder}
        handler (wrap-input-mold identity)
        context (assoc action-context :system sys)]
    (fact "adds :input-mold"
      (:input-mold (handler context)) => mold)
    (fact "requires system/mold-finder"
      (fact "no system"
        (handler raw-context) => (throws-error))
      (fact "no mold-finder"
        (handler (assoc-in context [:system :mold-finder] nil)) => (throws-error)))
    (fact "returns original data"
      (handler context) => (contains context))))


(def parsed-context 
  (assoc raw-context :input-mold mold :parsed-input parsed-input))
(def input {:price 123})

(fact "wrap-input"
  (let [handler (wrap-input identity)
        context parsed-context]
    (fact "turns parsed-input to input"
      (handler context) => (contains {:input input}))
    (fact "requires parsed-input"
      (handler (dissoc context :parsed-input)) => (throws-error))
    (fact "requires input-mold"
      (handler (dissoc context :input-mold)) => (throws-error))))

(def authorizor (>TestAuthorizer true))
(def input-context
  (assoc action-context :input input))


(fact "wrap-authorize"
  (let [sys {:authorizer authorizor}
        handler (wrap-authorize identity)
        context (assoc input-context :system sys)]
    (fact "requires system/authorizer"
      (fact "no system"
        (handler {}) => (throws-error))
      (fact "no authorizer"
        (handler raw-context) => (throws-error)))
    (fact "does nothing if authorized"
      (handler context) => context)
    (fact "throws exception if unauthorized"
      (let [unauth (>TestAuthorizer false)
            unauth-sys {:authorizer unauth}
            context (assoc input-context :system unauth-sys)]
        (handler context) => (throws-403)))
    (fact "returns original data"
      (handler context) => (contains context))))

(defn problem-action [context] (throw-problems {:name "bad"}))
(defn error-action [context] (throw-error))

(fact "wrap-output" 
  (let [handler (wrap-output identity)
        sys {}
        context (assoc input-context :system sys)]
    (fact "adds :output"
      (handler context) => (contains {:output anything}))
    (fact "requires action-sym"
      (handler (dissoc context :action-sym)) => (throws-error))
    (fact "calls action"
      (handler context) => (contains {:output output}))
    (fact "returns original data"
      (handler context) => (contains context))
    (let [bad-context (assoc context :action-sym `problem-action)]
      (fact "has __problems if problems thrown"
        (:output (handler bad-context)) => (contains {:__problems {:name "bad"}}))
      (fact "has original input if problems thrown"
        (:output (handler bad-context)) => (contains input))
      (fact "throws if non-problem exception"
        (let [err-context (assoc context :action-sym `error-action)]
          (handler err-context) => (throws))))))


(fact "wrap-output-mold"
  (let [sys {:mold-finder moldfinder}
        handler (wrap-output-mold identity)
        context (assoc input-context :system sys)]
    (fact "adds :output-mold"
      (handler context) => (contains {:output-mold mold}))
    (fact "requires system/mold-store"
      (fact "no system"
        (handler raw-context) => (throws-error))
      (fact "no moldstore"
        (handler (assoc-in context [:system :mold-finder] nil)) => (throws-error)))
    (fact "doesn't overwrite output-mold if it's already set"
      (handler (assoc context :output-mold 1)) => (contains {:output-mold 1}))
    (fact "returns original data"
      (handler context) => (contains context))))

(def molded-output {:price "$9.87"})
(def output-context (assoc input-context :output output :output-mold mold))

(fact "wrap-molded-output"
  (let [handler (wrap-molded-output identity)
        context output-context]
    (fact "turns output to molded-output"
      (handler context) => (contains {:molded-output molded-output}))
    (fact "requires output-mold"
      (handler (dissoc context :output-mold)) => (throws-error))
    (fact "requires output"
      (handler (dissoc context :output)) => (throws-error))))

(def molded-output-context 
  (assoc output-context :molded-output molded-output :output-mold mold))
(def rendered-output molded-output)

(fact "wrap-rendered-output"
  (let [handler (wrap-rendered-output identity)
        sys {:data-adapter data-adapter}
        context (assoc molded-output-context :system sys)]
    (fact "adds :rendered-output"
      (handler context) => (contains {:rendered-output rendered-output}))
    (fact "requires molded-output"
      (handler (dissoc context :molded-output)) => (throws-error))
    (fact "requires system/data-adapter"
      (fact "no system"
        (handler raw-context) => (throws-error))
      (fact "no data-adapter"
        (handler (assoc context :system {})) => (throws-error)))
    (fact "returns original data"
      (handler context) => (contains context))))


