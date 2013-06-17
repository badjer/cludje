(ns cludje.system)

(defprotocol IDatabase
  "Represents a datastore"
  (fetch- [self coll kee] "Get an item from the datastore")
  (query- [self coll params] "Get multiple items from the datastore. If params is empty, all entries should be returned")
  (write- [self coll kee data] "Insert or update. If kee is nil, insert. The key is returned")
  (delete- [self coll kee] "Delete"))

(defprotocol IMailer
  "Sends email"
  (send-mail- [self message] "Takes an email map. Expected keys are :from :to :subject :text :html"))

(defprotocol IAuth
  "Contols login and permissions"
  (current-user- [self] "Returns the currently logged-in user.")
  (login- [self user])
  (logout- [self user])
  (encrypt- [self txt] "Encrypt a string")
  (check-hash- [self txt cypher] "Test if the encrypted txt matches cypher")
  (in-role?- [self user role] "Is the user in the role?"))

(defprotocol ILogger
  "Represents logging"
  (log- [self message] "Log a message"))

(defprotocol IRouter
  "Does routing"
  (handle- [self request] "Handle a user request (a la Ring)"))

(defprotocol IRenderer
  "Handle finding and rendering output"
  (render- [self request output] "Generate output for the user"))
