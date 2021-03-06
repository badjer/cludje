(ns cludje.system
  (:use cludje.types
        cludje.mold
        cludje.model))

(defprotocol IAuthenticator
  "Contols identifying the user"
  (current-user [self context] "Returns the currently logged-in user.")
  (log-in [self context])
  (log-out [self context])
  (encrypt [self plaintext])
  (check-hash [self txt cypher]))

(defprotocol IDatastore
  "Represents a datastore"
  (fetch [self coll kee] "Get an item from the datastore")
  (query [self coll params] "Get multiple items from the datastore. If params is empty, all entries should be returned")
  (write [self coll kee data] "Insert or update. If kee is nil, insert. The key is returned")
  (delete [self coll kee] "Delete")
  (collections [self] "Get a list of all the collections in the db"))

(defn save [store model m]
  (let [parsed (make model m)
        kee (get m (keyname model))
        id (write store (tablename model) kee parsed)]
    {:_id id}))

(defn insert [store model m]
  (save store model (dissoc m (keyname model))))


(defmold MailMessage {:to Email :from Email :subject Str :body Str :text Str})

(defprotocol IEmailer
  "Sends email"
  (send-mailmessage [self message] "Takes an email map. Expected keys are :from :to :subject :text :html"))

(defn send-mail [emailer raw-message]
  (let [parsed (make MailMessage raw-message)]
    (send-mailmessage emailer parsed)))


(defprotocol ILog
  "Represents logging"
  (log [self message] "Log a message"))

(defprotocol IAuthorizer
  "Controls permissions"
  (can? [self context]))

(defprotocol IActionFinder
  (find-action [self context] "Get the action, or nil if it doesn't exist"))

(defprotocol IMoldFinder
  (find-output-mold [self context]))

(defprotocol IServer
  (start [self opts])
  (stop [self]))

(defprotocol ITemplateFinder
  (find-template [self context]))

