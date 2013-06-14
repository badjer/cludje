(ns cludje.system)

(defprotocol IDatabase
  "Represents a datastore"
  (fetch [self coll kee] "Get an item from the datastore")
  (query [self coll params] "Get multiple items from the datastore. If params is empty, all entries should be returned")
  (write! [self coll kee data] "Insert or update. If kee is nil, insert. The key is returned")
  (remove! [self coll kee] "Delete")
  (collections [self] "Get a seq of all the collections in the db")
  (drop-collection! [self coll] "Remove all entries in the collection"))

(defprotocol IMailer
  "Sends email"
  (send-mail [self message] "Takes an email map. Expected keys are :from :to :subject :text :html"))

(defprotocol IAuth
  "Contols login and permissions"
  (current-user [self] "Returns the currently logged-in user.
                        This should include any extra data - ie, company, etc.
                        It should also provide a list of roles.
                        This is modeled on the friend library")
  (login [self user])
  (logout [self user])
  (encrypt [self txt] "Encrypt a string")
  (check-hash [self txt cypher] "Test whether the encrypted txt matches cypher")
  (in-role? [self user role] "Is the user in the role?"))


(defprotocol ILogger
  "Represents logging"
  (log [self message] "Log a message"))

(defprotocol IRouter
  "Does routing"
  (handle [self request] "Handle a user request (a la Ring)"))

(defprotocol IRenderer
  "Handle finding and rendering output"
  (render [self request output] "Generate output for the user"))
