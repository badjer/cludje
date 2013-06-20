(ns cludje.core
  (:use cludje.types)
  (:require [clojure.string :as s]
            [cludje.validation]))

; ####
; Model protocols
; ####

(defprotocol IBuildable
  (build [self m] "Make an instance from something"))

(defn make 
  "Make an instance (calls build)"
  ([ibuildable]
   (build ibuildable nil))
  ([ibuildable m]
   (build ibuildable m))
  ([ibuildable f v & kvs]
   (build ibuildable (-> (apply hash-map kvs) 
                         (assoc f v)))))

(defprotocol IValidatable
  (problems? [self m] "Get a map of problems trying to make m"))

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
               (when-not (cludje.types/validate typ (get input field))
                 [field (str "Invalid format for " field)])))))

(defn- defmodel-problems [nam]
  (let [rec-name (record-name nam)]
    `(extend ~rec-name
       IValidatable
       {:problems? 
        (fn [self# m#] 
          (let [p# (get-problems (meta ~nam) m#)]
            (if-not (empty? p#)
              p#)))})))

(defn- defmodel-make [nam]
  (let [rec-name (record-name nam)
        constructor (symbol (str "->" rec-name))]
    `(extend ~rec-name
       IBuildable
       {:build 
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

(defprotocol IAuth
  "Contols login and permissions"
  (current-user- [self] "Returns the currently logged-in user.")
  (login- [self user])
  (logout- [self])
  (encrypt- [self txt] "Encrypt a string")
  (check-hash- [self txt cypher] "Test if the encrypted txt matches cypher")
  (in-role?- [self user role] "Is the user in the role?"))

(defprotocol IDispatcher
  (get-action- [self request] "Get the action to execute"))

(defprotocol IRenderer
  "Handle rendering output"
  (render- [self request output] "Generate output for the user"))


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
(defmodel AuthUser {:username Str :pwd Password} :no-key true)

(defn current-user [auth]
  (current-user- auth))
(defn login [auth user]
  (let [parsed (parse-model AuthUser user)]
    (login- auth parsed)))
(defn logout [auth]
  (logout- auth))
(defn encrypt [auth txt]
  (encrypt- auth txt))
(defn check-hash [auth txt cypher]
  (check-hash- auth txt cypher))
(defn in-role? [auth user role]
  (when role
    (let [kwrole (keyword (name role))]
      (in-role?- auth user kwrole))))


; Dispatcher api
(defn get-action [dispatcher request]
  (get-action- dispatcher request))



(defmacro defaction [nam & forms]
  `(defn ~nam [~'system ~'request]
     (let [~'save (partial save (:db ~'system))
           ~'fetch (partial fetch (:db ~'system))
           ~'query (partial query (:db ~'system))
           ~'write (partial write (:db ~'system))
           ~'delete (partial delete (:db ~'system))
           ~'send-mail (partial send-mail (:mailer ~'system))
           ~'log (partial log (:logger ~'system))
           ~'current-user (partial current-user (:auth ~'system))
           ~'login (partial login (:auth ~'system))
           ~'logout (partial logout (:auth ~'system))
           ~'encrypt (partial encrypt (:auth ~'system))
           ~'check-hash (partial check-hash (:auth ~'system))
           ~'in-role? (partial in-role? (:auth ~'system))]
       (try
         ~@forms
         (catch clojure.lang.ExceptionInfo ex#
           (let [problems# (:problems (ex-data ex#))]
             (assoc ~'request :problems problems#)))))))
