(ns cludje.core-test
  (:use midje.sweet
        cludje.test
        cludje.types
        cludje.database
        cludje.logger
        cludje.mailer
        cludje.auth
        cludje.login
        cludje.uiadapter
        cludje.modelstore
        cludje.app
        cludje.core))

(facts "?"
  (? {:a 1} :a) => 1
  (? {:a 1} :b) => (throws))

(facts "??"
  (?? {:a 1} :a) => 1
  (?? {:a 1} :b) => nil)

(facts "&?"
  (&? {:a 1} :a) => 1
  (&? {:a 1} :b :a) => 1
  (&? {:a 1}) => (throws)
  (&? {:a 1} :b) => (throws))

(defn myfun [] 1)
(defmodel User {:name Str :email Email :pwd Password})

(fact "find-in-ns"
  (find-in-ns 'cludje.core-test "myfun" nil) => fn?
  (find-in-ns 'cludje.core-test "asdfwefvasdf" nil) => nil
  (find-in-ns 'cludje.core-test "System" :cludje-model) => nil
  (find-in-ns 'cludje.core-test "User" :cludje-model) => User)

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

(fact "with-problem"
  (-> {} (with-problem :a "err")) => {:__problems {:a "err"}}
  (-> nil (with-problem :a "err")) => {:__problems {:a "err"}}
  (-> {:a 1} (with-problem :a "err")) => {:a 1 :__problems {:a "err"}}
  (-> {} (with-problem :a "err")) => (has-problems :a))

(fact "with-alert"
  (-> {} (with-alert :info "hi")) => {:__alerts [{:type :info :text "hi"}]}
  (-> nil (with-alert :info "hi")) => {:__alerts [{:type :info :text "hi"}]}
  (-> {:a 1} (with-alert :info "hi")) => 
    {:a 1 :__alerts [{:type :info :text "hi"}]}
  (-> {} (with-alert :info "hi")) => (has-alert :info #"hi"))

(defmodel Cog {:price Money :amt Int})
(defmodel Person {:name Str :age Int :_id Str} 
  :fieldnames {:age "How Old"}
  :require [:name]
  :invisible [:age]
  :defaults {:age 47})

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
    (:defaults (meta Person)) => {:age 47}
    (:partitions (meta Person)) => []
    (table-name User) => "user"
    (defaults User) => {}
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
    (problems? Person {}) => (just-keys :name))
  (fact "show"
    (show Cog {:amt 1 :price 4321}) => {:amt "1" :price "$43.21"}
    (show Cog nil) => nil
    (show Cog {}) => nil
    (fact "doesn't add any fields that weren't already there"
      (show Cog {:price 4321}) => {:price "$43.21"})))



(def cog {:price 123 :amt 1})
(def cog2 {:price 123 :amt 2})
(def cogb {:price 234 :amt 1})

(fact "write"
  (let [dba (atom {})
        db (->MemDb dba)
        sys {:db db}]
    (write sys Cog nil cog) => anything
    @dba => (just-keys :cog)
    (count (get @dba :cog)) => 1))

(fact "write will update"
  (let [db (->MemDb (atom {}))
        sys {:db db}
        id (write sys Cog nil cog)]
    (write sys Cog id cog2) => id
    (count (query sys Cog nil)) => 1))

(fact "fetch"
  (let [db (->MemDb (atom {}))
        sys {:db db}
        id (write sys Cog nil cog)]
    (fetch sys Cog id) => (contains cog)
    (fetch sys Cog nil) => nil
    (fetch sys nil nil) => nil
    (fetch sys nil id) => nil
    (fact "works with a map as the key"
      (fetch sys Cog {:_id id}) => (contains cog)
      (fetch sys Cog {:_id nil}) => nil)))

(fact "fetch with multiple rows"
  (let [db (->MemDb (atom {}))
        sys {:db db}
        id (write sys Cog nil cog)
        idb (write sys Cog nil cogb)]
    (fetch sys Cog id) => (contains cog)
    (fetch sys Cog idb) => (contains cogb)))


(fact "save"
  (let [db (->MemDb (atom {}))
        sys {:db db}]
    (fact "save exceptions"
      (save sys Cog {}) => (throws Exception)
      (save sys Cog {:price 123}) => (throws Exception)
      (save sys Cog {:price 123 :amt 1}) => anything
      (save sys Cog {:price "abc" :amt 1}) => (throws Exception)
      (save sys Cog {:price 123 :amt "a"}) => (throws Exception))
    (fact "save with extra fields is fine"
      (save sys Cog {:price 123 :amt 1 :x 1}) => anything)
    (fact "save returns something key-like"
      (save sys Cog cog) =not=> empty?)))

(fact "save will set the key field"
  (let [dba (atom {})
        db (->MemDb dba)
        sys {:db db}
        kee (save sys Cog cog)]
    (count (:cog @dba)) => 1
    (first (:cog @dba)) => (contains kee)
    (fetch sys Cog (:_id kee)) => (contains kee)))

(fact "save knows when to insert"
  (let [db (->MemDb (atom {}))
        sys {:db db}
        id (write sys Cog nil cog)]
    (save sys Cog cog) =not=> id
    (count (query sys Cog nil)) => 2))

(fact "save knows when to update"
  (let [db (->MemDb (atom {}))
        sys {:db db}
        kee (save sys Cog cog)]
    (count (query sys Cog nil)) => 1
    (save sys Cog (merge cog kee)) => anything
    (count (query sys Cog nil)) => 1))

(fact "insert"
  (let [db (->MemDb (atom {}))
        sys {:db db}]
    (fact "insert exceptions"
      (insert sys Cog {}) => (throws Exception)
      (insert sys Cog {:price 123}) => (throws Exception)
      (insert sys Cog {:price 123 :amt 1}) => anything
      (insert sys Cog {:price "abc" :amt 1}) => (throws Exception)
      (insert sys Cog {:price 123 :amt "a"}) => (throws Exception))
    (fact "insert with extra fields is fine"
      (insert sys Cog {:price 123 :amt 1 :x 1}) => anything)
    (fact "insert returns something key-like"
      (insert sys Cog cog) =not=> empty?)))

(fact "insert will set the key field"
  (let [dba (atom {})
        db (->MemDb dba)
        sys {:db db}
        kee (insert sys Cog cog)]
    (count (:cog @dba)) => 1
    (first (:cog @dba)) => (contains kee)
    (fetch sys Cog (:_id kee)) => (contains kee)))

(fact "insert knows when to insert"
  (let [db (->MemDb (atom {}))
        sys {:db db}
        id (write sys Cog nil cog)]
    (insert sys Cog cog) =not=> id
    (count (query sys Cog nil)) => 2))

(fact "insert knows when to update"
  (let [db (->MemDb (atom {}))
        sys {:db db}
        id (insert sys Cog cog)]
    (count (query sys Cog nil)) => 1
    (insert sys Cog cog) => anything
    (count (query sys Cog nil)) => 2))

(facts "db operations work with keyword table names"
  (let [db (->MemDb (atom {}))
        sys {:db db}
        id (write sys :cog nil cog)]
    (count (query sys :cog nil)) => 1
    (fetch sys :cog id) => (contains cog)
    (delete sys :cog id) => anything
    (query sys :cog nil) => nil))

(facts "save and insert return a map"
  (let [dba (atom {})
        db (->MemDb dba)
        sys {:db db}]
    (insert sys Cog cog) => map?
    (save sys Cog cog) => map?))

(facts "db-writing ops return maps"
  (let [db (->MemDb (atom {}))
        sys {:db db}]
    (save sys Cog cog) => map?
    (save sys Cog cog) => (has-keys :_id)
    (:_id (save sys Cog cog)) => string?
    (insert sys Cog cog) => map?
    (insert sys Cog cog) => (has-keys :_id)
    (:_id (insert sys Cog cog)) => string?))


(fact "get-key"
  (get-key Cog {:_id 1}) => 1
  (get-key Cog {}) => nil)

(fact "throw-problems"
  (throw-problems {:a 1}) => (throws)
  (try
    (throw-problems {:a 1})
    (catch Exception ex
      (ex-data ex) => (has-keys :__problems)
      (:__problems (ex-data ex)) => {:a 1})))

(fact "save knows when to update"
  (let [db (->MemDb (atom {}))
        sys {:db db}
        id (write sys Cog nil cog)
        with-kee (assoc cog :_id id)]
    (save sys Cog with-kee) => (contains {:_id id})
    (count (query sys Cog nil)) => 1))

(def mail {:to "a@b.cd" :from "b@b.cd" :subject "test"
           :body "hi" :text "hi"})

(fact "send-mail"
  (let [mailatom (atom [])
        mailer (->MemMailer mailatom)
        sys {:mailer mailer}]
    (send-mail sys mail) =not=> (throws)
    (send-mail sys nil) => (throws)
    (send-mail sys (assoc mail :to nil)) => (throws)
    (send-mail sys (assoc mail :to "abcd")) => (throws)
    (send-mail sys (dissoc mail :to)) => (throws)
    (send-mail sys (dissoc mail :from)) => (throws)
    (send-mail sys (dissoc mail :subject)) => (throws)
    (send-mail sys (dissoc mail :text)) => (throws)
    (send-mail sys (dissoc mail :body)) => (throws)))


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
(defaction ident-insert insert)
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
    (fact "defaction defines a new insert"
      ((ident-insert sys nil) Cog cog) =not=> (throws))
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
      (add-cog sys cog) => ok?
      (count (query sys Cog nil)) => 1
      (first (query sys Cog nil)) => (contains cog))
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
      (add-person sys (merge person usr)) => ok?
      (count (query sys User nil)) => 1
      (count (query sys Person nil)) => 1)))

(defaction add-2-persons
  (save Person input)
  (save Person input))

(fact "defaction runs all operations in body (defaction has implicit do)"
  (let [dba (atom {})
        db (->MemDb dba)
        sys {:db db}]
    (add-2-persons sys person) => anything
    (count (query sys Person nil)) => 2))


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
  (current-user nil nil) => nil)

(facts "login"
  (let [logn (make-MockLogin {:logged-in? false})
        sys {:login logn}]
    (fact "login works with extra fields"
      (current-user sys nil) => nil
      (login sys (assoc mockuser :fluff 1)) => anything
      (current-user sys nil) => mockuser)
    (fact "login throws exception if missing fields"
      (logout sys nil) => anything
      (login sys (dissoc mockuser :pwd)) => (throws)
      (login sys (dissoc mockuser :username)) => (throws))))

(defaction ident-current-user current-user)
(defaction ident-login login)
(defaction ident-logout logout)
(defaction ident-encrypt encrypt)

(facts "defaction login api"
  (let [logn (make-MockLogin {:logged-in? false})
        sys {:login logn}]
    ((ident-current-user sys nil)) =not=> (throws)
    ((ident-login sys nil) mockuser) =not=> (throws)
    ((ident-logout sys nil) nil) =not=> (throws)
    ((ident-encrypt sys nil) "a") =not=> (throws)))

(defaction ident-authorize authorize)
(defaction ident-can? can?)

(facts "defaction auth api"
  (let [auth (make-auth mock-auth-fn)
        sys {:auth auth}]
    ((ident-authorize sys nil) :add :model mockuser :guest) =not=> (throws)
    ((ident-can? sys nil) :add :model {}) =not=> (throws)))

(defaction ac-authorize (authorize :action Cog (current-user) input))
(defaction ac-can? (can? :action Cog input))

(defaction ac-current-user (current-user))
(defaction ac-login (login input))
(defaction ac-logout (logout input))
(defaction ac-encrypt (encrypt input))

(facts "defaction login and auth api works"
  (let [logn (make-MockLogin {:logged-in? false})
        auth (make-auth mock-auth-fn)
        sys {:login logn :auth auth}]
    (ac-login sys mockuser) => ok?
    (ac-current-user sys nil) => mockuser
    (ac-logout sys nil) => ok?
    (ac-current-user sys nil) => nil
    (ac-encrypt sys "a") => "a"
    ; We require the user to be logged in for the rest of the tests
    (ac-login sys mockuser) => anything
    (ac-authorize sys {:b 1}) => true
    (ac-can? sys nil) => truthy
    ; We require the user to be not logged in for the rest of the tests
    (ac-logout sys nil) => anything
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

(defaction ac-&?
  (&? :a :b))

(facts "&? in action dsl"
  (ac-&? nil {:a 1}) => 1
  (ac-&? nil {:b 2}) => 2
  (ac-&? nil {:c 3}) => (has-problems :a :b))


(defability ab-cog
  :add Cog (= (:amt input) 1))

(facts "defability"
  (ab-cog nil :add Cog mockuser {:amt 1}) => truthy
  (ab-cog nil :add Cog mockuser {:amt 2}) => falsey
  (ab-cog nil :remove Cog mockuser {:amt 1}) => falsey
  (ab-cog nil :add Cog mockuser {}) => falsey
  (ab-cog nil "add" Cog mockuser {:amt 1}) => truthy
  (ab-cog nil "add" Cog mockuser {:amt 2}) => falsey)

(future-facts "defability should work with strs in do-action"
  (ab-cog nil :add "Cog" mockuser {:amt 1}) => truthy
  (ab-cog nil :add "Cog" mockuser {:amt 2}) => falsey
  (ab-cog nil :add "cog" mockuser {:amt 1}) => truthy
  (ab-cog nil :add "cog" mockuser {:amt 2}) => falsey
  (ab-cog nil "add" "cog" mockuser {:amt 1}) => truthy
  (ab-cog nil "add" "cog" mockuser {:amt 2}) => falsey)

(defability ab-str-model
  :add "Foosum" true)

(facts "defability works with strs in ability"
  (ab-str-model nil :add "Foosum" mockuser nil) => truthy
  (ab-str-model nil :list "Foosum" mockuser nil) => falsey
  (ab-str-model nil :add "Foobar" mockuser nil) => falsey)
  
(defability ab-all-cog
  :add Cog true)

(facts "defability with no filter"
  (ab-all-cog nil :add Cog mockuser {:amt 1}) => truthy
  (ab-all-cog nil :add Cog mockuser nil) => truthy
  (ab-all-cog nil :remove Cog mockuser {:amt 1}) => falsey)

(defability ab-star-cog
  :* Cog true)

(facts "defability with *"
  (ab-star-cog nil :add Cog mockuser nil) => truthy
  (ab-star-cog nil :foo Cog mockuser nil) => truthy
  (ab-star-cog nil :add Person mockuser nil) => falsey)

(defability ab-star-override-cog
  :delete Cog false
  :* Cog true)

(facts "defability earlier entry overrides later one"
  (ab-star-override-cog nil :add Cog mockuser nil) => truthy
  (ab-star-override-cog nil :delete Cog mockuser nil) => falsey)

(defn is-amt-1 [x] (= 1 (:amt x)))

(defability ab-cog-var
  :add Cog (is-amt-1 cog))

(facts "defability with var"
  (ab-cog-var nil :add Cog mockuser {:amt 1 :price 1}) => truthy
  (ab-cog-var nil :add Cog mockuser {:amt 2 :price 1}) => falsey
  (ab-cog-var nil :remove Cog mockuser {:amt 1 :price 1}) => falsey
  (facts "ability with var doesn't need the entire object supplied"
    ;That will be the responsibility of validation
    (ab-cog-var nil :add Cog mockuser {:amt 1}) => truthy
    (ab-cog-var nil :add Cog mockuser {:amt 2}) => falsey))

(defability ab-cog-bare-fn
  :add Cog is-amt-1)

(defn sys-amt-fn [system input]
  (and (contains? system :db) (= 1 (:amt input))))

(defability ab-cog-bare-fn2
  :add Cog sys-amt-fn)

(facts "defability with bare fns"
  (ab-cog-bare-fn nil :add Cog mockuser {:amt 1}) => truthy
  (ab-cog-bare-fn nil :add Cog mockuser {:amt 2}) => falsey
  (ab-cog-bare-fn nil :remove Cog mockuser {:amt 1}) => falsey
  (ab-cog-bare-fn2 {:db nil} :add Cog mockuser {:amt 1}) => truthy
  (ab-cog-bare-fn2 {} :add Cog mockuser {:amt 1}) => falsey
  (ab-cog-bare-fn2 nil :add Cog mockuser {:amt 1}) => falsey
  (ab-cog-bare-fn2 {:db nil} :add Cog mockuser {:amt 2}) => falsey)

(defability ab-cog-person
  :add Cog (is-amt-1 cog)
  :add Person (= (:name person) (:username user)))

(facts "defability with multiple permissions"
  (ab-cog-person nil :add Cog mockuser {:amt 1}) => truthy
  (ab-cog-person nil :add Cog mockuser {:amt 2}) => falsey
  (ab-cog-person nil :remove Cog mockuser {:amt 1}) => falsey
  (ab-cog-person nil :add Person {:username "a"} {:name "a"}) => truthy
  (ab-cog-person nil :add Person {:username "a"} {:name "b"}) => falsey
  (ab-cog-person nil :remove Person {:username "a"} {:name "a"}) => falsey
  (ab-cog-person nil :add Person {:username "b"} {:name "a"}) => falsey)

(defability ab-ac-vector
  [:add :delete] Cog true)

(facts "defability with a vector of actions"
  (ab-ac-vector nil :add Cog mockuser cog) => truthy
  (ab-ac-vector nil :delete Cog mockuser cog) => truthy
  (ab-ac-vector nil :list Cog mockuser cog) => falsey
  (ab-ac-vector nil :add Person mockuser person) => falsey)

(defability ab-cog-anon
  :add Cog :anon)

(facts "defability only allows anonymous access for :anon"
  (ab-cog-anon nil :add Cog mockuser cog) => truthy
  (ab-all-cog nil :add Cog nil cog) => falsey)

(defability ab-dsl
  :add Cog (= 0 (count (query Cog {}))))

(fact "defability can use action dsl functions"
  (let [db (->MemDb (atom {}))
        sys {:db db}]
    (ab-dsl sys :add Cog mockuser nil) => truthy
    (insert sys Cog cog) => anything
    (ab-dsl sys :add Cog mockuser nil) => falsey))

(facts "auth works with defability"
  (let [auth (make-auth ab-cog)
        logn (make-MockLogin {:logged-in? true})
        sys {:auth auth :login logn}]
    (current-user sys nil) =not=> nil?
    (can? sys :add Cog {:amt 1}) => true
    (can? sys :add Cog {:amt 2}) => falsey
    (can? sys :delete Cog {:amt 1}) => falsey))

(defaction cog-add {:_id 1})
(defaction cog-forbidden {:secret "foo"})

(fact "do-action"
  (let [sys (make-system {:login (make-MockLogin {:logged-in? true})
                          :logger (->MemLogger (atom []))
                          :default-action nil
                          :action-ns 'cludje.core-test
                          :model-ns 'cludje.core-test
                          :uiadapter (->TestUIAdapter (atom nil))
                          :auth (make-auth ab-ac-vector)})]
    (do-action sys {:_action "cog-add"}) => {:_id 1}
    (fact "Not found action"
      (do-action sys {:_action "cog-foobarzums"}) => (throws)
      (try (do-action sys {:_action "cog-foobarzums"})
        (catch clojure.lang.ExceptionInfo ex
          (ex-data ex))) => (has-keys :__notfound))
    (fact "Forbids access if no permissions"
      (do-action sys {:_action "cog-forbidden"}) => (throws)
      (try (do-action sys {:_action "cog-forbidden"})
        (catch clojure.lang.ExceptionInfo ex
          (ex-data ex))) => (has-keys :__unauthorized))
    (fact "Not allowed if not logged in"
      (logout sys nil) => anything
      (do-action sys {:_action "cog-add"}) => (throws)
      (try (do-action sys {:_action "cog-add"})
        (catch clojure.lang.ExceptionInfo ex
          (ex-data ex))) => (has-keys :__notloggedin))
    ))

(fact "do-action writes errors to log"
  (let [log (atom [])
        sys (make-system {:logger (->MemLogger log)
                          :login (make-MockLogin {:logged-in? true})
                          :default-action nil
                          :action-ns 'cludje.core-test
                          :model-ns 'cludje.core-test
                          :uiadapter (->TestUIAdapter (atom nil))
                          :auth (make-auth ab-ac-vector)})]
    (do-action sys {:_action "cog-forbidden"}) => (throws)
    (count @log) => 1
    (last @log) => #"^Unauthorized"
    (do-action sys {:_action "cog-foobarzums"}) => (throws)
    (last @log) => #"^Not found"
    (logout sys nil) => anything
    (do-action sys {:_action "cog-add"}) => (throws)
    (last @log) => #"Not logged in"))

(defaction anon-act {:a 1})
(defaction nonanon-act {:a 1})
(defability ab-anon
  :act "Anon" :anon
  :act "Nonanon" true
  :* "Global" :anon)

(defaction global-login (login input))
(defaction global-logout (logout input))

(fact "do-action with anonymous auth"
  (let [sys (make-system {:login (make-MockLogin {:logged-in? false})
                          :logger (->MemLogger (atom []))
                          :default-action nil
                          :action-ns 'cludje.core-test
                          :model-ns 'cludje.core-test
                          :uiadapter (->TestUIAdapter (atom nil))
                          :auth (make-auth ab-anon)})]
    (do-action sys {:_action "anon-act"}) => {:a 1}
    (do-action sys {:_action "nonanon-act"}) => (throws-401)
    (login sys mockuser) => anything
    (do-action sys {:_action "anon-act"}) => {:a 1}
    (do-action sys {:_action "nonanon-act"}) => {:a 1}))

(fact "do-action with token login"
  (let [sys-a (make-system {:logger (->MemLogger (atom []))
                            :db (->MemDb (atom {:user [mockuser]}))
                          :default-action nil
                          :action-ns 'cludje.core-test
                          :model-ns 'cludje.core-test
                          :uiadapter (->TestUIAdapter (atom nil))
                          :auth (make-auth ab-anon)})
        sys (assoc sys-a :login (->TokenLogin "1" (:db sys-a) :user))]
    (do-action sys {:_action "nonanon-act"}) => (throws-401)
    (do-action sys (merge mockuser {:_action :global-login})) => ok?
    (do-action sys {:_action "nonanon-act"}) => {:a 1}
    (fact "run-action works too"
      (run-action sys nonanon-act {}) => {:a 1}
      (fact "run-action doesn't need auth"
        (do-action sys {:_action :global-logout}) => ok?
        (run-action sys nonanon-act {}) => {:a 1}))))
