(ns cludje.core-test
  (:use midje.sweet
        cludje.test
        cludje.types
        cludje.database
        cludje.logger
        cludje.mailer
        cludje.auth
        cludje.dispatcher
        cludje.renderer
        cludje.core))

(defmodel User {:name Str :email Email :pwd Password})
(defmodel Cog {:price Money :amt Int})
(defmodel Person {:name Str :age Int :user_id Str} :require [:name])

(fact "defmodel"
  (fact "metadata"
    (meta User) => (has-keys :fields :require :table)
    (:fields (meta User)) => (has-keys :name :email :pwd)
    (:email (:fields (meta User))) => Email
    (:require (meta User)) => [:name :email :pwd]
    (:table (meta User)) => "user")
  (fact "make"
    User =not=> nil
    User => (has-keys :name :email :pwd)
    (make User {:name "abc"}) => (has-keys :name :email :pwd)
    (make User {:name "abc"}) => (contains {:name "abc"})
    (make User :name "abc") => (contains {:name "abc"})
    (make User :name "abc" :email "d@e.fg") => 
      (contains {:name "abc" :email "d@e.fg"}))
  (fact "make adds key"
    (make User {}) => (has-keys :user_id))
  (fact "problems? checks field existence"
    (problems? User {}) =not=> empty?
    (problems? User {}) => (has-keys :name :email :pwd)
    (problems? User {:name "a"}) => (just-keys :email :pwd))
  (fact "problems? returns empty if no problems"
    (problems? Person {:name "abc" :age 2}) => nil)
  (fact "make returns a map with all keys"
    (make User {}) => (has-keys :name :email :pwd)
    (make User {:name "a"}) => (has-keys :name :email :pwd))
  (fact "make removes any extra keys"
    (make User {:foosums "a"}) => (just-keys :name :email :pwd :user_id))
  (fact "parse converts field values"
    (make Cog {:amt "12"}) => (contains {:amt 12})
    (make Cog {:price "$12.34"}) => (contains {:price 1234}))
  (fact "problems? checks type"
    (problems? Cog {:amt "asdf"}) => (has-keys :amt))
  (fact "problems? only needs required fields"
    (problems? Person {}) => (just-keys :name)))

(def cog {:price 123 :amt 1})
(def cog2 {:price 123 :amt 2})
(def cogb {:price 234 :amt 1})

(fact "write"
  (let [dba (atom {})
        db (->MemDb dba)]
    (write db Cog nil cog) => anything
    @dba => (just-keys :cog)
    (count (get @dba :cog)) => 1))

(fact "write will update"
  (let [db (->MemDb (atom {}))
        id (write db Cog nil cog)]
    (write db Cog id cog2) => id
    (count (query db Cog nil)) => 1))


(fact "fetch"
  (let [db (->MemDb (atom {}))
        id (write db Cog nil cog)]
    (fetch db Cog id) => (contains cog)
    (fetch db Cog nil) => nil
    (fetch db nil nil) => nil
    (fetch db nil id) => nil))

(fact "fetch with multiple rows"
  (let [db (->MemDb (atom {}))
        id (write db Cog nil cog)
        idb (write db Cog nil cogb)]
    (fetch db Cog id) => (contains cog)
    (fetch db Cog idb) => (contains cogb)))


(fact "save"
  (let [db (->MemDb (atom {}))]
    (fact "save exceptions"
      (save db Cog {}) => (throws Exception)
      (save db Cog {:price 123}) => (throws Exception)
      (save db Cog {:price 123 :amt 1}) => anything
      (save db Cog {:price "abc" :amt 1}) => (throws Exception)
      (save db Cog {:price 123 :amt "a"}) => (throws Exception))
    (fact "save with extra fields is fine"
      (save db Cog {:price 123 :amt 1 :x 1}) => anything)
    (fact "save returns something key-like"
      (save db Cog cog) =not=> empty?)))

(fact "save knows when to insert"
  (let [db (->MemDb (atom {}))
        id (write db Cog nil cog)]
    (save db Cog cog) =not=> id
    (count (query db Cog nil)) => 2))

(fact "get-key"
  (get-key Cog {:cog_id 1}) => 1
  (get-key Cog {}) => nil)

(fact "throw-problems"
  (throw-problems {:a 1}) => (throws)
  (try
    (throw-problems {:a 1})
    (catch Exception ex
      (ex-data ex) => (has-keys :problems)
      (:problems (ex-data ex)) => {:a 1})))

(fact "save knows when to update"
  (let [db (->MemDb (atom {}))
        id (write db Cog nil cog)
        with-id (assoc cog :cog_id id)]
    (save db Cog with-id) => id
    (count (query db Cog nil)) => 1))

(def mail {:to "a@b.cd" :from "b@b.cd" :subject "test"
           :body "hi" :text "hi"})

(fact "send-mail"
  (let [mailatom (atom [])
        mailer (->MemMailer mailatom)]
    (send-mail mailer mail) =not=> (throws)
    (send-mail mailer nil) => (throws)
    (send-mail mailer (assoc mail :to nil)) => (throws)
    (send-mail mailer (assoc mail :to "abcd")) => (throws)
    (send-mail mailer (dissoc mail :to)) => (throws)
    (send-mail mailer (dissoc mail :from)) => (throws)
    (send-mail mailer (dissoc mail :subject)) => (throws)
    (send-mail mailer (dissoc mail :text)) => (throws)
    (send-mail mailer (dissoc mail :body)) => (throws)))


(defaction ident request)
(defaction ident-sys system)

(fact "defaction base api functions"
  (let [sys {}] 
    (fact "defaction creates a method with 2 params" 
      (ident nil nil) =not=> (throws))
    (fact "defaction has a request argument"
      (ident nil cog) => cog)
    (fact "defaction has a system argument"
      (ident-sys sys nil) => sys)))

(defaction ident-save save)
(defaction ident-fetch fetch)
(defaction ident-query query)
(defaction ident-write write)
(defaction ident-delete delete)

(fact "defaction db api functions"
  (let [db (->MemDb (atom {}))
        sys {:db db}]
    (fact "defaction defines a new save"
      ; There should be a save method, with a smaller arity 
      ; (the first argument should already be bound)
      ((ident-save sys nil) Cog cog) =not=> (throws))
    (fact "defaction defines a new fetch"
      ((ident-fetch sys nil) Cog nil) =not=> (throws))
    (fact "defaction defines a new query"
      ((ident-query sys nil) Cog nil) =not=> (throws))
    (fact "defaction defines a new write"
      ((ident-write sys nil) Cog nil cog) =not=> (throws))
    (fact "defaction defines a new delete"
      ((ident-delete sys nil) Cog nil) =not=> (throws))))

(defaction add-cog (save Cog request))

(fact "defaction db api functionality"
  (let [db (->MemDb (atom {}))
        sys {:db db}]
    (fact "defaction can save"
      (add-cog sys cog) =not=> has-problems?
      (count (query db Cog nil)) => 1
      (first (query db Cog nil)) => (contains cog))
    (fact "defaction returns problems if save fails"
      (add-cog sys {}) => has-problems?
      (add-cog sys {}) => (has-problems :price :amt))))

(defaction add-person
  (let [uid (save User request)
        dt (assoc request :user_id uid)]
    (save Person dt)))

(def person {:name "a" :age 2})
(def user {:name "a" :email "a@b.cd" :pwd "123"})

(fact "complex db defaction"
  (let [db (->MemDb (atom {}))
        sys {:db db}]
    (fact "exception in let returns problems"
      (add-person sys {}) => (has-problems :name))
    (fact "multiple operations work"
      (add-person sys (merge person user)) =not=> has-problems?
      (count (query db User nil)) => 1
      (count (query db Person nil)) => 1)))


(defaction ident-send-mail send-mail)

(fact "defaction mail api"
  (let [mailatom (atom [])
        mailer (->MemMailer mailatom)
        sys {:mailer mailer}]
    (fact "send-mail exists"
      ((ident-send-mail sys nil) mail) =not=> (throws))))

(defaction send-an-email (send-mail request))

(fact "defaction mail api works"
  (let [mailatom (atom [])
        mailer (->MemMailer mailatom)
        sys {:mailer mailer}]
    (fact "send-mail executes"
      (send-an-email sys mail) => anything
      (count @mailatom) => 1
      (first @mailatom) => mail)))

(defaction ident-log log)

(fact "defaction log api"
  (let [logger (->MemLogger (atom []))
        sys {:logger logger}]
    (fact "log exists"
      ((ident-log sys nil) "hi") =not=> (throws))))

(defaction log-request (log request))

(fact "defaction log api works"
  (let [logatom (atom [])
        logger (->MemLogger logatom)
        sys {:logger logger}]
    (fact "log executes"
      (log-request sys "hi") => anything
      (count @logatom) => 1
      (first @logatom) => "hi")))


(facts "login"
  (let [auth (->MockAuth (atom false))]
    (fact "login works with extra fields"
      (current-user auth) => nil
      (login auth (assoc mockuser :fluff 1)) => anything
      (current-user auth) => mockuser)
    (fact "login throws exception if missing fields"
      (logout auth) => anything
      (login auth (dissoc mockuser :pwd)) => (throws)
      (login auth (dissoc mockuser :username)) => (throws))))

(facts "in-role?"
  (let [auth (->MockAuth (atom false))]
    (in-role? auth mockuser nil) => falsey
    (in-role? auth nil :guest) => falsey
    (fact "in-role? with keyword roles"
      (in-role? auth mockuser :guest) => truthy
      (in-role? auth mockuser :a) => falsey)
    (fact "in-role? with string roles"
      (in-role? auth mockuser "guest") => truthy
      (in-role? auth mockuser "a") => falsey)))

(defaction ident-current-user current-user)
(defaction ident-login login)
(defaction ident-logout logout)
(defaction ident-encrypt encrypt)
(defaction ident-check-hash check-hash)
(defaction ident-in-role? in-role?)

(facts "defaction auth api"
  (let [auth (->MockAuth (atom false))
        sys {:auth auth}]
    ((ident-current-user sys nil)) =not=> (throws)
    ((ident-login sys nil) mockuser) =not=> (throws)
    ((ident-logout sys nil)) =not=> (throws)
    ((ident-encrypt sys nil) "a") =not=> (throws)
    ((ident-check-hash sys nil) "a" "a") =not=> (throws)
    ((ident-in-role? sys nil) mockuser :guest) =not=> (throws)))

(defaction ac-current-user (current-user))
(defaction ac-login (login request))
(defaction ac-logout (logout))
(defaction ac-encrypt (encrypt request))
(defaction ac-check-hash (check-hash request "a"))
(defaction ac-in-role? (in-role? request :guest))

(facts "defaction auth api works"
  (let [logged-in? (atom true)
        auth (->MockAuth logged-in?)
        sys {:auth auth}]
    (ac-login sys mockuser) =not=> has-problems?
    @logged-in? => true
    (ac-current-user sys nil) => mockuser
    (ac-logout sys nil) =not=> has-problems?
    @logged-in? => false
    (ac-encrypt sys "a") => "a"
    (ac-check-hash sys "a") => true
    (ac-check-hash sys "b") => false
    (ac-in-role? sys mockuser) => truthy
    (ac-in-role? sys nil) => falsey))

; Dispatcher api
; Currently, we're not making this easily available to
; defaction, because I'm not sure it'd ever be needed

(defaction ident-render render)

(facts "defaction render api"
  (let [rr (->LiteralRenderer)
        sys {:renderer rr}]
    (ident-render sys nil) =not=> (throws)
    (ident-render sys nil) => fn?))

(defaction ac-render1 (render {:a 2}))
(defaction ac-render0 (render))

(facts "defaction render api works"
  (let [rr (->LiteralRenderer)
        sys {:renderer rr}]
    (ac-render1 sys {:a 1}) => {:a 2}
    (ac-render0 sys {:a 1}) => {:a 1}))

(defaction ac-a1 {:a 1})
    
(facts "make-ring-handler"
  (let [rr (->LiteralRenderer)
        disp (->Dispatcher (atom {:ident ident :a1 ac-a1}))
        sys {:renderer rr :dispatcher disp}
        handler (make-ring-handler sys)]
    handler => fn?
    (handler {:action :ident}) => {:action :ident}
    (handler {:action :a1}) => {:a 1}))

