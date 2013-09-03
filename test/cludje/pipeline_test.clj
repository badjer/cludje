(ns cludje.pipeline-test
  (:use midje.sweet
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

(def unparsed-input {:price "$1.23"})
(def raw-context {:unparsed-input unparsed-input})

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
        (handler (dissoc context :system)) => (throws))
      (fact "no session-store"
        (handler (assoc context :system {})) => (throws)))
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
    (fact "adds :input"
      (handler context) => (contains {:parsed-input parsed-input}))
    (fact "requires system/data-adapter"
      (fact "no system"
        (handler raw-context) => (throws))
      (fact "no data-adapter"
        (handler (assoc context :system {})) => (throws)))
    (fact "requires unparsed-input"
      (handler (dissoc context :unparsed-input)) => (throws))
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
        (handler {}) => (throws))
      (fact "no authenticator"
        (handler raw-context) => (throws)))
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
        (handler {}) => (throws))
      (fact "no action-finder"
        (handler raw-context) => (throws)))
    (fact "returns original data"
      (handler context) => (contains context))))

(def action-context (assoc raw-context :user user :action-sym `action))
(def mold (>Mold {:price Money} {}))
(def moldfinder (>SingleMoldFinder `mold))

(fact "wrap-input-mold"
  (let [sys {:mold-finder moldfinder}
        handler (wrap-input-mold identity)
        context (assoc action-context :system sys)]
    (fact "adds :input-mold"
      (handler context) => (contains {:input-mold-sym `mold}))
    (fact "requires system/mold-finder"
      (fact "no system"
        (handler raw-context) => (throws))
      (fact "no moldstore"
        (handler (assoc-in context [:system :mold-finder] nil)) => (throws)))
    (fact "returns original data"
      (handler context) => (contains context))))

(def parsed-context 
  (assoc raw-context :input-mold-sym `mold :parsed-input parsed-input))
(def input {:price 123})

(fact "wrap-input"
  (let [handler (wrap-input identity)
        context parsed-context]
  (fact "turns parsed-input to input"
    (handler context) => (contains {:input input}))
  (fact "requires parsed-input"
    (handler (dissoc context :parsed-input)) => (throws))
  (fact "requires input-mold-sym"
    (handler (dissoc context :input-mold-sym)) => (throws))))

(def authorizor (>TestAuthorizer true))
(def input-context
  (assoc action-context :input input))


(fact "wrap-authorize"
  (let [sys {:authorizer authorizor}
        handler (wrap-authorize identity)
        context (assoc input-context :system sys)]
    (fact "requires system/authorizer"
      (fact "no system"
        (handler {}) => (throws))
      (fact "no authorizer"
        (handler raw-context) => (throws)))
    (fact "does nothing if authorized"
      (handler context) => context)
    (fact "throws exception if unauthorized"
      (let [unauth (>TestAuthorizer false)
            unauth-sys {:authorizer unauth}
            context (assoc input-context :system unauth-sys)]
        (handler context) => (throws)))
    (fact "returns original data"
      (handler context) => (contains context))))

(fact "wrap-output" 
  (let [handler (wrap-output identity)
        sys {}
        context (assoc input-context :system sys)]
    (fact "adds :output"
      (handler context) => (contains {:output anything}))
    (fact "requires action-sym"
      (handler (dissoc context :action-sym)) => (throws))
    (fact "calls action"
      (handler context) => (contains {:output output}))
    (fact "returns original data"
      (handler context) => (contains context))))

(fact "wrap-output-mold"
  (let [sys {:mold-finder moldfinder}
        handler (wrap-output-mold identity)
        context (assoc input-context :system sys)]
    (fact "adds :output-mold"
      (handler context) => (contains {:output-mold-sym `mold}))
    (fact "requires system/mold-store"
      (fact "no system"
        (handler raw-context) => (throws))
      (fact "no moldstore"
        (handler (assoc-in context [:system :mold-finder] nil)) => (throws)))
    (fact "returns original data"
      (handler context) => (contains context))))

(def molded-output {:price "$9.87"})
(def output-context (assoc input-context :output output :output-mold-sym `mold))

(fact "wrap-molded-output"
  (let [handler (wrap-molded-output identity)
        context output-context]
    (fact "turns output to molded-output"
      (handler context) => (contains {:molded-output molded-output}))
    (fact "requires output-mold"
      (handler (dissoc context :output-mold-sym)) => (throws))
    (fact "requires output"
      (handler (dissoc context :output)) => (throws))))

(def molded-output-context 
  (assoc output-context :molded-output molded-output :output-mold-sym `mold))
(def rendered-output molded-output)

(fact "wrap-rendered-output"
  (let [handler (wrap-rendered-output identity)
        sys {:data-adapter data-adapter}
        context (assoc molded-output-context :system sys)]
    (fact "adds :rendered-output"
      (handler context) => (contains {:rendered-output rendered-output}))
    (fact "requires molded-output"
      (handler (dissoc context :molded-output)) => (throws))
    (fact "requires system/data-adapter"
      (fact "no system"
        (handler raw-context) => (throws))
      (fact "no data-adapter"
        (handler (assoc context :system {})) => (throws)))
    (fact "returns original data"
      (handler context) => (contains context))))


