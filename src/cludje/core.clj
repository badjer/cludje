(ns cludje.core
  (:use cludje.types
        [cludje.validation :only [IValidateable validate-test validate problems?]])
  (:require [cludje.types]
            [clojure.string :as s]
            [cludje.validation]
            [ring.middleware.resource :as resource]
            [ring.middleware.file-info :as file-info]
            [ring.middleware.keyword-params :as kw]
            [ring.middleware.json :as json]))

; ####
; Model builder
; ####

(defn make 
  "Make an instance from a map (calls parse).
  This is used to parse models nicely"
  ([iparseable]
   (parse iparseable nil))
  ([iparseable m]
   (parse iparseable m))
  ([iparseable f v & kvs]
   (parse iparseable (-> (apply hash-map kvs) 
                         (assoc f v)))))


; ####
; Model construction
; ####

(defn- record-name [nam]
  (symbol (str (name nam) "-type")))

(defn- defmodel-record [nam fields]
  (let [kees (map #(symbol (name %)) (keys fields))
        rec-name (record-name nam)]
    `(defrecord ~rec-name [~@kees])))

(defn- defmodel-singleton [nam fields opts]
  (let [rec-name (record-name nam)
        constructor (symbol (str "->" rec-name))
        numkeys (count (keys fields))]
    `(def ~nam (with-meta 
                 (apply ~constructor (repeat ~numkeys nil))
                 ~opts))))

(defn get-problems [model-meta input]
  (merge
    (apply cludje.validation/needs input (:require model-meta))
    (into {} (for [[field typ] (:fields model-meta)]
               (when-not (validate typ (get input field))
                 [field (str "Invalid format for " field)])))))

(defn- defmodel-problems [nam]
  (let [rec-name (record-name nam)]
    `(extend ~rec-name
       IValidateable
       {:problems? 
        (fn [self# m#] 
          (let [p# (get-problems (meta ~nam) m#)]
            (if-not (empty? p#)
              p#)))})))

(defn- defmodel-make [nam]
  (let [rec-name (record-name nam)
        constructor (symbol (str "->" rec-name))]
    `(extend ~rec-name
       IParseable
       {:parse 
        (fn [self# m#]
          (let [parsed# 
                (into {} 
                      (for [[field# typ#] (:fields (meta ~nam))] 
                        [field# (cludje.types/parse typ# (get m# field#))]))]
            (merge
              (apply ~constructor (repeat (count (keys ~nam)) nil))
              parsed#)))})))


(defmacro defmodel [nam fields & opts]
  (let [optmap (apply hash-map opts)
        table (s/lower-case (name nam))
        no-key? (:no-key optmap)
        kee (if no-key? 
              nil
              (keyword (str table "_id")))
        ; Required fields don't include key
        reqfields (vec (keys fields))
        allfields (if no-key? 
                    fields 
                    (assoc fields kee cludje.types/Str))
        modelopts (merge {:require reqfields
                          :fields allfields
                          :table table
                          :key kee}
                         optmap)]
    `(do
       ~(defmodel-record nam fields)
       ~(defmodel-singleton nam fields modelopts)
       ~(defmodel-problems nam)
       ~(defmodel-make nam)
       )))


; ####
; System protocols
; ####
(defprotocol IDatabase
  "Represents a datastore"
  (fetch- [self coll kee] "Get an item from the datastore")
  (query- [self coll params] "Get multiple items from the datastore. If params is empty, all entries should be returned")
  (write- [self coll kee data] "Insert or update. If kee is nil, insert. The key is returned")
  (delete- [self coll kee] "Delete"))

(defprotocol IMailer
  "Sends email"
  (send-mail- [self message] "Takes an email map. Expected keys are :from :to :subject :text :html"))

(defprotocol ILogger
  "Represents logging"
  (log- [self message] "Log a message"))

(defprotocol ILogin
  "Contols login"
  (current-user- [self] "Returns the currently logged-in user.")
  (login- [self user])
  (logout- [self])
  (encrypt- [self txt] "Encrypt a string")
  (check-hash- [self txt cypher] "Test if the encrypted txt matches cypher")
  )

(defprotocol IAuth
  "Controls permissions"
  (authorize- [self action model user input] "Is the user allowed to do this?"))

(defprotocol IDispatcher
  (get-action- [self request] "Get the action to execute"))

(defprotocol IRenderer
  "Handle rendering output"
  (render- [self request output] "Generate output for the user"))

(defprotocol IServer
  (set-handler- [self handler]))

(defprotocol IStartable
  (start- [self])
  (stop- [self]))

; TODO: Do something with this
;(defprotocol IPersistent
;(get-state [self])
;(init-state [self state]))





; ####
; API 
; ####

; DB API
(defn- table-name [model]
  (:table (meta model)))

(defn- key-name [model]
  (:key (meta model)))

(defn fetch [db model kee]
  (let [tbl (table-name model)]
    (fetch- db tbl kee)))

(defn query [db model params]
  (let [tbl (table-name model)]
    (query- db tbl params)))

(defn write [db model kee data]
  (let [tbl (table-name model)]
    (write- db tbl kee data)))

(defn delete [db model kee]
  (let [tbl (table-name model)]
    (delete- db tbl kee)))

(defn throw-problems 
  ([]
   (throw-problems {}))
  ([problems]
   (throw (ex-info "Problems" {:problems problems}))))

(defn get-key [model m]
  (get m (key-name model) nil))

(defn parse-model [model m]
  (if-let [probs (problems? model m)]
    (throw-problems probs)
    (make model m)))

(defn save [db model m]
  (let [parsed (parse-model model m)
        kee (get-key model parsed)] 
    (write db model kee parsed)))


; Logger api
(defn log [logger s]
  (log- logger s))


; Mail api
(defmodel MailMessage 
  {:to Email :from Email :subject Str :body Str :text Str}
  :no-key true)

(defn send-mail [mailer mes]
  (let [parsed (parse-model MailMessage mes)]
    (send-mail- mailer parsed)))


; Auth api
(defmodel LoginUser {:username Str :pwd Password} :no-key true)

(defn current-user [auth]
  (when auth
    (current-user- auth)))
(defn login [auth user]
  (let [parsed (parse-model LoginUser user)]
    (login- auth parsed)))
(defn logout [auth]
  (logout- auth))
(defn encrypt [auth txt]
  (encrypt- auth txt))
(defn check-hash [auth txt cypher]
  (check-hash- auth txt cypher))

(defn authorize [auth action model user input]
  (authorize- auth action model user input))

;(defn arity [f]
;(let [m (first (.getDeclaredMethods (class f)))
;p (.getParameterTypes m)]
;(alength p)))


(defn- match-ability? [auth-action auth-model expr]
  `(let [~(symbol (s/lower-case (name auth-model))) (make ~auth-model ~'input)]
     (and (= ~'action ~auth-action) 
          (= ~'model ~auth-model) 
          ~expr)))

(defmacro defability [nam & forms]
  "Creates a function that can be used to authorize access to a model"
  (let [calls (for [[auth-action auth-model expr] (partition 3 forms)]
                (match-ability? auth-action auth-model expr))]
    `(do
       (defn ~nam [~'action ~'model ~'user ~'input]
         (or ~@calls))
       (alter-meta! (var ~nam) assoc :ability true))))


(defn can? [auth logn action model m]
  (let [user (current-user logn)]
    (authorize auth action model user m)))

; Dispatcher api
(defn get-action [dispatcher request]
  (get-action- dispatcher request))


; Renderer api
(defn render [renderer request output]
  (render- renderer request output))


; Request api
(defn ? 
  "Returns kee from input, throwing an exception if it's not found,
  or if the fn pred fails when applied to the value.
  The exception will contain problem data, so that it will be
  caught by the handler if defaction, meaning errors won't 
  crash an action"
  ([input kee]
   (? input kee (constantly true)))
  ([input kee pred] 
   (when-not (contains? input kee)
     (throw-problems {kee (str " is required but was not provided")}))
   (let [v (get input kee)]
     (if (validate-test pred v)
       v
       (throw-problems {kee (str kee " was not valid")})))))

(defn ?? 
  "Returns kee from input, but does NOT throw an exception if it's
  not found (unlike ?) - return default (or nil) instead"
  ([input kee]
   (?? input kee nil))
  ([input kee default]
    (get input kee default)))



(defmacro defaction [nam & forms]
  `(do
     (defn ~nam [~'system ~'input]
       (let [~'save (partial save (:db ~'system))
             ~'fetch (partial fetch (:db ~'system))
             ~'query (partial query (:db ~'system))
             ~'write (partial write (:db ~'system))
             ~'delete (partial delete (:db ~'system))
             ~'send-mail (partial send-mail (:mailer ~'system))
             ~'log (partial log (:logger ~'system))
             ~'current-user (partial current-user (:login ~'system))
             ~'login (partial login (:login ~'system))
             ~'logout (partial logout (:login ~'system))
             ~'encrypt (partial encrypt (:login ~'system))
             ~'check-hash (partial check-hash (:login ~'system))
             ~'authorize (partial authorize (:auth ~'system))
             ~'can? (partial can? (:auth ~'system) (:login ~'system))
             ~'user (~'current-user)
             ~'? (partial ? ~'input)
             ~'?? (partial ?? ~'input)]
         (try
           ~@forms
           (catch clojure.lang.ExceptionInfo ex#
             (let [problems# (:problems (ex-data ex#))]
               (assoc ~'input :problems problems#)))))) 
     (alter-meta! (var ~nam) assoc :action true)))

(defn make-ring-handler [{:keys [dispatcher renderer] :as system}] 
  (-> (fn [request] 
        (when-let [action (get-action dispatcher request)] 
          (let [input (:params request)
                output (action system input)] 
            (render renderer request output))))
      (resource/wrap-resource "public")
      (file-info/wrap-file-info) 
      (kw/wrap-keyword-params) 
      (json/wrap-json-params)))


; System stuff

(defn start-system [sys]
  (let [handler (make-ring-handler sys)]
    ; Set the server handler
    (set-handler- (:server sys) handler)
    (doseq [subsys (vals sys)]
      (when (extends? IStartable (type subsys))
        (start- subsys)))
    sys))


(defn stop-system [sys]
  (doseq [subsys (vals sys)]
    (when (extends? IStartable (type subsys))
      (stop- subsys))))
