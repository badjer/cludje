(ns cludje.core
  (:use cludje.types)
  (:require [clojure.string :as s]))

(defn- find-ns-var [nas thing]
  (when (and nas thing)
    (cond 
      (symbol? thing) (ns-resolve nas thing)
      (keyword? thing) (ns-resolve nas (symbol (name thing)))
      (= "" thing) nil
      :else (ns-resolve nas (symbol thing)))))

(defn find-in-ns 
  "Find the var thing in the namespace nas that has metadata-filter.
  If metadata-filter is nil, return anything with that name"
  ([nas thing] (find-in-ns nas thing nil))
  ([nas thing metadata-filter]
    (when-let [vr (find-ns-var nas thing)]
      (when (var? vr)
        (when (or (nil? metadata-filter) (metadata-filter (meta @vr)))
          @vr)))))


(defn throw-problems 
  ([]
   (throw-problems {}))
  ([problems]
   (throw (ex-info "Problems" {:__problems problems}))))

(defn throw-unauthorized 
  ([] (throw-unauthorized {}))
  ([details]
    (throw (ex-info "Unauthorized" 
                    (merge {:__unauthorized "Unauthorized"} details)))))

(defn throw-not-found 
  ([] (throw-not-found {}))
  ([details]
    (throw (ex-info "Not found" 
                    (merge {:__notfound "Not found"} details)))))

(defn throw-not-logged-in 
  ([] (throw-not-logged-in {}))
  ([details]
    (throw (ex-info "Not logged in" 
                    (merge {:__notloggedin "Not logged in"} details)))))

(defn with-alert [m text typ]
  (update-in m [:__alerts] conj {:text text :type typ}))


(defn friendly-name 
  ([model field]
   (if-let [names (:fieldnames (meta model))]
     (get names field)
     (friendly-name field)))
  ([field] (s/capitalize (name field))))


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
     (throw-problems {kee (str (friendly-name kee) " is required but was not provided")}))
   (let [v (get input kee)]
     (if (validate-test pred v)
       v
       (throw-problems {kee (str (friendly-name kee) " was not valid")})))))

(defn ?? 
  "Returns kee from input, but does NOT throw an exception if it's
  not found (unlike ?) - return default (or nil) instead"
  ([input kee]
   (?? input kee nil))
  ([input kee default]
   (get input kee default)))


(defn table-name [model]
  (cond
    (keyword? model) model
    (nil? model) nil
    (= java.lang.String (type model)) (s/lower-case model)
    :else (? (meta model) :table)))

(defn model-name [s]
  (cond
    (keyword? s) (s/capitalize (name s))
    (nil? s) nil
    (= java.lang.String (type s)) (s/capitalize s)
    :else (s/capitalize (? (meta s) :table))))

(defn key-name [model] :_id)

(defn field-types [model]
  (? (meta model) :fields))

(defn needs [data & kees]
  "Generate an error if any of the supplied keys is missing from data"
  (apply merge
         (for [kee kees]
           (if-not (value? (kee data)) 
             {kee (str "Please supply a value for " (friendly-name kee))}))))

(defn bad [f x]
  "Returns true only if x has a value and f also fails"
  (and (value? x) (not (validate-test f x))))

(defn no-bad [f m & kees]
  "Returns a map of errors if any of the supplied kees are bad f"
  (apply merge
         (for [kee kees]
           (if (bad f (get m kee))
             {kee (str "Can't understand " (friendly-name kee))}))))

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
    `(def ~nam 
       (with-meta (apply ~constructor (repeat ~numkeys nil)) 
                  ~opts))))

(defn get-problems [model input]
  (merge
    (apply needs input (:require (meta model)))
    (into {} (for [[field typ] (:fields (meta model))]
               (when-not (validate typ (get input field))
                 [field (str "Invalid format for " 
                             (friendly-name model field))])))))

(defn- defmodel-problems [nam]
  (let [rec-name (record-name nam)]
    `(extend ~rec-name
       IValidateable
       {:problems? 
        (fn [self# m#] 
          (let [p# (get-problems ~nam m#)]
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
                      (for [[field# typ#] (field-types ~nam)]
                        [field# (parse typ# (get m# field#))]))]
            (merge
              (apply ~constructor (repeat (count (keys ~nam)) nil))
              parsed#)))})))


(defmacro defmodel [nam fields & opts]
  (let [optmap (apply hash-map opts)
        table (s/lower-case (name nam))
        no-key? (:no-key optmap)
        kee (if no-key?  nil :_id)
        ; Required fields don't include key
        reqfields (vec (keys fields))
        allfields (if no-key? 
                    fields 
                    (assoc fields kee (symbol "Str")))
        fieldnames (merge 
                     (zipmap (keys allfields) 
                             (map friendly-name (keys allfields)))
                     (get optmap :fieldnames {}))
        invisible (conj (get optmap :invisible []) :_id)
        modelopts (merge {:require reqfields
                          :cludje-model true
                          :fields allfields
                          :fieldnames fieldnames
                          :table table
                          :invisible invisible
                          :key kee}
                         (dissoc optmap :fieldnames :invisible))]
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
  (current-user- [self input] "Returns the currently logged-in user.")
  (login- [self input])
  (logout- [self input])
  (encrypt- [self txt] "Encrypt a string")
  (check-hash- [self txt cypher] "Test if the encrypted txt matches cypher")
  )

(defprotocol IAuth
  "Controls permissions"
  (authorize- [self action model user input] "Is the user allowed to do this?"))

(defprotocol IActionParser
  (get-action-name- [self input] "Get the full name of the action from input")
  (get-model-name- [self input] "Get the name of the model associated with the action")
  (get-action-key- [self input] "Get the keyword/crud (ie, :add) portion of the action name"))

(defprotocol IActionStore
  (get-action- [self action-name] "Get the action, or nil if it doesn't exist"))

(defprotocol IModelStore
  (get-model- [self modelname] "Get the model, or nil if it doesn't exist"))

(defprotocol IServer
  (set-handler- [self handler]))

(defprotocol IStartable
  (start- [self])
  (stop- [self]))



; #####
; UI protocols
; #####
(defprotocol IRenderer
  "Handle rendering output"
  (render- [self request output] "Generate output for the user"))

(defprotocol IInputParser
  "Handle converting input into our standard input format"
  (parse-input- [self request]))



; TODO: Do something with this
;(defprotocol IPersistent
;(get-state [self])
;(init-state [self state]))




; ####
; API 
; ####

; DB API
(defn fetch [db model kee]
  (when model
    (let [tbl (table-name model)]
      (fetch- db tbl kee))))

(defn query [db model params]
  (let [tbl (table-name model)]
    (query- db tbl params)))

(defn write [db model kee data]
  (let [tbl (table-name model)] 
    (write- db tbl kee data)))

(defn delete [db model kee]
  (when kee
    (let [tbl (table-name model)]
      (delete- db tbl kee)
      kee)))


(defn get-key [model m]
  (get m (key-name model) nil))

(defn parse-model [model m]
  (if-let [probs (problems? model m)]
    (throw-problems probs)
    (make model m)))

(defn save [db model m]
  (let [parsed (parse-model model m)
        kee (get-key model parsed)
        id (write db model kee parsed)]
    {:_id id}))

(defn insert [db model m]
  (save db model (dissoc m (key-name model))))


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

(defn current-user [auth input]
  (when auth
    (current-user- auth input)))
(defn login [auth input]
  ; Do parsing so that we validate that we have the right fields
  (parse-model LoginUser input)
  (login- auth input))
(defn logout [auth input]
  (logout- auth input))
(defn encrypt [auth txt]
  (encrypt- auth txt))

(defn authorize [auth action model user input]
  (authorize- auth action model user input))


;(defn arity [f]
;(let [m (first (.getDeclaredMethods (class f)))
;p (.getParameterTypes m)]
;(alength p)))

;(declare action-allowed?)
;(defn- action-allowed-seq? [auth-action action]
  ;(for [a auth-action]
    ;(action-allowed auth-action action))


(defn action-matches? [auth-action action]
  (or 
    (= "*" (name auth-action))
    (= (name auth-action) (name action))))


(defn- match-ability? [auth-action auth-model expr]
  `(let [~(symbol (s/lower-case (name auth-model))) 
         (if (= (type ~auth-model) java.lang.String)
           nil
           (make ~auth-model ~'input))]
     (when (and (action-matches? ~auth-action ~'action) 
                (= ~'model ~auth-model))
       (and (or (not (nil? ~'user))
                (= :anon ~expr))
            ~expr))))
                ; We either need to be logged in, or have
                ; anonymous access granted
                ;(or (not (nil? ~'user))
                    ;(= :anon ~expr)))
          ;~expr)))

(defn- parse-action-forms [forms]
  (apply concat
         (for [[action model expr] (partition 3 forms)]
           (if (vector? action)
             (for [act action] (match-ability? act model expr))
             [(match-ability? action model expr)]))))

(defmacro defability [nam & forms]
  "Creates a function that can be used to authorize access to a model"
  (let [calls (parse-action-forms forms)]
    `(do
       (def ~nam (with-meta (fn [~'action ~'model ~'user ~'input]
         (first (keep identity [~@calls])))
                       {:cludje-ability true})))))


(defn can? [auth logn action model input]
  (let [user (current-user logn input)]
    (authorize auth action model user input)))



(defmacro defaction [nam & forms]
  `(do
     (def ~nam (with-meta (fn [~'system ~'input]
       (let [~'save (partial save (:db ~'system))
             ~'insert (partial insert (:db ~'system))
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
             ~'authorize (partial authorize (:auth ~'system))
             ~'can? (partial can? (:auth ~'system) (:login ~'system))
             ~'user (~'current-user ~'input)
             ~'? (partial ? ~'input)
             ~'?? (partial ?? ~'input)]
         (try
           ~@forms
           (catch clojure.lang.ExceptionInfo ex#
             (if-let [problems# (:__problems (ex-data ex#))]
               (->
                 (assoc ~'input :__problems problems#)
                 (with-alert "There were problems" :error))
               (throw ex#))))))
                          {:cludje-action true}))))

(defn error-unauthorized [{:keys [logger] :as system} details]
  (log logger (str "Unauthorized: " details))
  (throw-unauthorized details))

(defn error-not-found [{:keys [logger] :as system} details]
  (log logger (str "Not found: " details))
  (throw-not-found details))

(defn error-not-logged-in [{:keys [logger] :as system} details]
  (log logger (str "Not logged in: " details))
  (throw-not-logged-in details))


(defaction do-action
  (let [action (->> (get-action-name- (:actionparser system) input)
                   (get-action- (:actionstore system)))
        action-key (get-action-key- (:actionparser system) input)
        model-name (get-model-name- (:actionparser system) input)
        model (get-model- (:modelstore system) model-name)]
    (cond 
      (nil? action) 
        (error-not-found system {:model model-name :action action-key})
      (not (authorize action-key (or model model-name) user input)) 
        (if (nil? user) 
          (error-not-logged-in system {})
          (error-unauthorized system {:model model-name :action action-key}))
      :else (action system input))))
