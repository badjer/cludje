(ns cludje.core-test
  (:use midje.sweet
        cludje.test
        cludje.types
        cludje.database
        cludje.logger
        cludje.mailer
        cludje.auth
        cludje.login
        cludje.dispatcher
        cludje.renderer
        cludje.core))

(fact "needs should return an entry for each missing field"
  (needs {} :a) => (has-keys :a)
  (needs {} :a :b) => (has-keys :a :b)
  (needs {:a 1} :a) => falsey
  (needs {:a ""} :a) => (has-keys :a)
  (needs {:a nil} :a) => (has-keys :a)
  (needs {:a 1} :b) => (has-keys :b)
  (needs {:a 1 :b 2} :a :b) => falsey)

(fact "bad should work with a cludje type"
  (bad Email "a") => truthy
  (bad Email "a@b.cd") => falsey)

(fact "bad should work with a fn"
  (bad even? 1) => truthy
  (bad even? 2) => falsey)

(fact "bad should return truthy only if the value? and not the given test"
  (bad Email nil) => falsey
  (bad Email "") => falsey
  (bad Email "a") => truthy
  (bad Email "a@b.cd") => falsey)

(fact "no-bad should return an entry for each field that 
      isn't null and is invlid"
  (no-bad Email {} :a) => falsey
  (no-bad Email {:a ""} :a) => falsey
  (no-bad Email {:a "a"} :a) => (has-keys :a)
  (no-bad Email {:a "a@b.cd"} :a) => falsey)

(defmodel User {:name Str :email Email :pwd Password})
(defmodel Cog {:price Money :amt Int})
(defmodel Person {:name Str :age Int :_id Str} 
  :fieldnames {:age "How Old"}
  :require [:name]
  :invisible [:age])

(def User-copy User)

(fact "defmodel"
  (fact "metadata"
    (meta User) => (has-keys :fields :require :table)
    (:fields (meta User)) => (has-keys :name :email :pwd)
    (:email (:fields (meta User))) => Email
    (:require (meta User)) => [:name :email :pwd]
    (:table (meta User)) => "user"
    (:fieldnames (meta User)) => (has-keys :name :email :pwd)
    (:fieldnames (meta Person)) => (contains {:age "How Old"})
    (:invisible (meta Person)) => [:age :_id]
    (table-name User) => "user"
    (key-name User) => :_id
    (field-types User) => (has-keys :name :email :pwd))
  (fact "metadata on another var"
    (meta User-copy) => (has-keys :fields :require :table)
    (table-name User-copy) => "user"
    (key-name User-copy) => :_id)
  (fact "make"
    User =not=> nil
    User => (has-keys :name :email :pwd)
    (make User {:name "abc"}) => (has-keys :name :email :pwd)
    (make User {:name "abc"}) => (contains {:name "abc"})
    (make User :name "abc") => (contains {:name "abc"})
    (make User :name "abc" :email "d@e.fg") => 
      (contains {:name "abc" :email "d@e.fg"}))
  (fact "make adds key"
    (make User {}) => (has-keys :_id))
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
    (make User {:foosums "a"}) => (just-keys :name :email :pwd :_id))
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

(fact "save will set the key field"
  (let [dba (atom {})
        db (->MemDb dba)
        id (save db Cog cog)]
    (count (:cog @dba)) => 1
    (first (:cog @dba)) => (has-keys :_id)
    (:_id (first (:cog @dba))) => id
    (fetch db Cog id) => (contains {:_id id})))

(fact "save knows when to insert"
  (let [db (->MemDb (atom {}))
        id (write db Cog nil cog)]
    (save db Cog cog) =not=> id
    (count (query db Cog nil)) => 2))

(fact "save knows when to update"
  (let [db (->MemDb (atom {}))
        id (save db Cog cog)]
    (count (query db Cog nil)) => 1
    (save db Cog cog) => anything
    (count (query db Cog nil)) => 2))

(facts "db operations work with keyword table names"
  (let [db (->MemDb (atom {}))
        id (write db :cog nil cog)]
    (count (query db :cog nil)) => 1
    (fetch db :cog id) => (contains cog)
    (delete db :cog id) => anything
    (query db :cog nil) => nil))

(facts "db ops with keywords and models update same table"
  (let [dba (atom {})
        db (->MemDb dba)]
    (write db Cog nil cog) => anything
    @dba => (just {:cog anything})
    (write db :cog nil cog) => anything
    @dba => (just {:cog anything})))


(fact "get-key"
  (get-key Cog {:_id 1}) => 1
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
        with-id (assoc cog :_id id)]
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


(defaction ident input)
(defaction ident-sys system)

(fact "defaction base api functions"
  (let [sys {}] 
    (fact "defaction creates a method with 2 params" 
      (ident nil nil) =not=> (throws))
    (fact "defaction has a input argument"
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

(defaction add-cog (save Cog input))

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
  (let [uid (save User input)
        dt (assoc input :_id uid)]
    (save Person dt)))

(def person {:name "a" :age 2})
(def usr {:name "a" :email "a@b.cd" :pwd "123"})

(fact "complex db defaction"
  (let [db (->MemDb (atom {}))
        sys {:db db}]
    (fact "exception in let returns problems"
      (add-person sys {}) => (has-problems :name))
    (fact "multiple operations work"
      (add-person sys (merge person usr)) =not=> has-problems?
      (count (query db User nil)) => 1
      (count (query db Person nil)) => 1)))

(defaction add-2-persons
  (save Person input)
  (save Person input))

(fact "defaction runs all operations in body (defaction has implicit do)"
  (let [dba (atom {})
        db (->MemDb dba)
        sys {:db db}]
    (add-2-persons sys person) => anything
    (count (query db Person nil)) => 2))


(defaction ident-send-mail send-mail)

(fact "defaction mail api"
  (let [mailatom (atom [])
        mailer (->MemMailer mailatom)
        sys {:mailer mailer}]
    (fact "send-mail exists"
      ((ident-send-mail sys nil) mail) =not=> (throws))))

(defaction send-an-email (send-mail input))

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

(defaction log-input (log input))

(fact "defaction log api works"
  (let [logatom (atom [])
        logger (->MemLogger logatom)
        sys {:logger logger}]
    (fact "log executes"
      (log-input sys "hi") => anything
      (count @logatom) => 1
      (first @logatom) => "hi")))

(fact "current-user works with nil as :auth"
  (current-user nil) => nil)

(facts "login"
  (let [logn (make-MockLogin false)]
    (fact "login works with extra fields"
      (current-user logn) => nil
      (login logn (assoc mockuser :fluff 1)) => anything
      (current-user logn) => mockuser)
    (fact "login throws exception if missing fields"
      (logout logn) => anything
      (login logn (dissoc mockuser :pwd)) => (throws)
      (login logn (dissoc mockuser :username)) => (throws))))

(defaction ident-current-user current-user)
(defaction ident-login login)
(defaction ident-logout logout)
(defaction ident-encrypt encrypt)
(defaction ident-check-hash check-hash)
(defaction ident-user user)

(facts "defaction login api"
  (let [logn (make-MockLogin false)
        sys {:login logn}]
    ((ident-current-user sys nil)) =not=> (throws)
    ((ident-login sys nil) mockuser) =not=> (throws)
    ((ident-logout sys nil)) =not=> (throws)
    ((ident-encrypt sys nil) "a") =not=> (throws)
    ((ident-check-hash sys nil) "a" "a") =not=> (throws)
    (ident-user sys nil) =not=> (throws)))

(defaction ident-authorize authorize)
(defaction ident-can? can?)

(facts "defaction auth api"
  (let [auth (make-auth mock-auth-fn)
        sys {:auth auth}]
    ((ident-authorize sys nil) :action :model mockuser :guest) =not=> (throws)
    ((ident-can? sys nil) :action :model {}) =not=> (throws)))

(defaction ac-authorize (authorize :action Cog user input))
(defaction ac-can? (can? :action Cog input))

(defaction ac-current-user (current-user))
(defaction ac-login (login input))
(defaction ac-logout (logout))
(defaction ac-encrypt (encrypt input))
(defaction ac-check-hash (check-hash input "a"))
(defaction ac-user user)

(facts "defaction login and auth api works"
  (let [logn (make-MockLogin false)
        auth (make-auth mock-auth-fn)
        sys {:login logn :auth auth}]
    (ac-login sys mockuser) =not=> has-problems?
    (ac-current-user sys nil) => mockuser
    (ac-logout sys nil) =not=> has-problems?
    (ac-current-user sys nil) => nil
    (ac-encrypt sys "a") => "a"
    (ac-check-hash sys "a") => true
    (ac-check-hash sys "b") => false
    ; We require the user to be logged in for the rest of the tests
    (ac-login sys mockuser) => anything
    (ac-user sys nil) => mockuser
    (ac-authorize sys {:b 1}) => true
    (ac-can? sys nil) => truthy
    ; We require the user to be not logged in for the rest of the tests
    (ac-logout sys nil) => anything
    (ac-user sys nil) => nil
    (ac-authorize sys nil) => falsey
    (ac-can? sys nil) => falsey))

(defaction ac-?? (?? :a))

(facts "??"
  (ac-?? nil {:a 1}) => 1
  (ac-?? nil {:b 1}) => nil
  (ac-?? nil nil) => nil
  (ac-?? nil {}) => nil)

(defaction ac-??-default (?? :a -1))

(facts "?? with default value"
  (ac-??-default nil {:a 1}) => 1
  (ac-??-default nil {:b 1}) => -1
  (ac-??-default nil nil) => -1
  (ac-??-default nil {}) => -1)

(defaction ac-? (? :a))

(facts "?"
  (ac-? nil {:a 1}) => 1
  (ac-? nil {:b 1}) => (has-problems :a)
  (ac-? nil {}) => (has-problems :a)
  (ac-? nil nil) => (has-problems :a))

(defaction ac-?-validator (? :a Int))

(facts "? with validator"
  (ac-?-validator nil {:a 1}) => 1
  (ac-?-validator nil {:a "abc"}) => (has-problems :a)
  (ac-?-validator nil {:b 1}) => (has-problems :a)
  (ac-?-validator nil nil) => (has-problems :a)
  (ac-?-validator nil {}) => (has-problems :a))



(defability ab-cog
  :add Cog (= (:amt input) 1))

(facts "defability"
  (ab-cog :add Cog nil {:amt 1}) => true
  (ab-cog :add Cog nil {:amt 2}) => false
  (ab-cog :remove Cog nil {:amt 1}) => false
  (ab-cog :add Cog nil {}) => false)
  
(defability ab-all-cog
  :add Cog true)

(facts "defability with no filter"
  (ab-all-cog :add Cog nil {:amt 1}) => true
  (ab-all-cog :add Cog nil nil) => true
  (ab-all-cog :remove Cog nil {:amt 1}) => false)

(defn is-amt-1 [x] (= 1 (:amt x)))

(defability ab-cog-var
  :add Cog (is-amt-1 cog))

(facts "ability with var"
  (ab-cog-var :add Cog nil {:amt 1 :price 1}) => true
  (ab-cog-var :add Cog nil {:amt 2 :price 1}) => false
  (ab-cog-var :remove Cog nil {:amt 1 :price 1}) => false
  (facts "ability with var doesn't need the entire object supplied"
    ;That will be the responsibility of validation
    (ab-cog-var :add Cog nil {:amt 1}) => true
    (ab-cog-var :add Cog nil {:amt 2}) => false))

(defability ab-cog-person
  :add Cog (is-amt-1 cog)
  :add Person (= (:name person) (:username user)))

(facts "defability with multiple permissions"
  (ab-cog-person :add Cog nil {:amt 1}) => true
  (ab-cog-person :add Cog nil {:amt 2}) => false
  (ab-cog-person :remove Cog nil {:amt 1}) => false
  (ab-cog-person :add Person {:username "a"} {:name "a"}) => true
  (ab-cog-person :add Person {:username "a"} {:name "b"}) => false
  (ab-cog-person :remove Person {:username "a"} {:name "a"}) => false
  (ab-cog-person :add Person {:username "b"} {:name "a"}) => false)

(defability ab-ac-vector
  [:add :delete] Cog true)

(facts "defability with a vector of actions"
  (ab-ac-vector :add Cog nil cog) => true
  (ab-ac-vector :delete Cog nil cog) => true
  (ab-ac-vector :list Cog nil cog) => false
  (ab-ac-vector :add Person nil person) => false)

(facts "auth works with defability"
  (let [auth (make-auth ab-cog)
        logn (make-MockLogin true)]
    (can? auth logn :add Cog {:amt 1}) => true
    (can? auth logn :add Cog {:amt 2}) => falsey
    (can? auth logn :delete Cog {:amt 1}) => falsey))

  

