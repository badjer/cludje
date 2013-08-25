(ns cludje.system)

(defprotocol IAuthenticator
  "Contols identifying the user"
  (current-user [self context] "Returns the currently logged-in user.")
  (log-in [self context user])
  (log-out [self context]))

(defprotocol IDatastore
  "Represents a datastore"
  (fetch2 [self coll kee] "Get an item from the datastore")
  (query2 [self coll params] "Get multiple items from the datastore. If params is empty, all entries should be returned")
  (write2 [self coll kee data] "Insert or update. If kee is nil, insert. The key is returned")
  (delete2 [self coll kee] "Delete"))

(defprotocol IEmailer
  "Sends email"
  (send-mail [self message] "Takes an email map. Expected keys are :from :to :subject :text :html"))

(defprotocol ILog
  "Represents logging"
  (log2 [self message] "Log a message"))

(defprotocol IAuthorizer
  "Controls permissions"
  (allowed? [self system action-sym user input] 
            "Is the user allowed to do this?"))

(defprotocol IActionFinder
  (find-action [self context] "Get the action, or nil if it doesn't exist"))

(defprotocol IMoldStore
  (get-mold [self context]))

(defprotocol IDataAdapter
  (parse-input [self rawinput])
  (render-output [self output]))
