(ns cludje.auth-test
  (:use midje.sweet
        cludje.types
        cludje.testcontrollers
        cludje.core
        cludje.auth))

(defmodel Bar {:foo Str})
(def mockuser {:username "a@b.cd" :pwd "123"})

(fact "auth"
  (let [auth (make-auth mock-auth-fn)]
    (authorize auth :add :model {} {}) => truthy
    (authorize auth :add :model nil {}) => falsey))

(fact "find-abilities"
  (let [abs (find-abilities 'cludje.testcontrollers)
        authfn (apply make-auth-fn abs)]
    (count abs) => 2
    authfn => fn?
    ((first abs) :add Foo mockuser 1) => truthy
    (authfn :add Foo mockuser 1) => truthy
    (fact "second ability in controller also works"
      (authfn :remove Foo mockuser 1) => truthy)
    (authfn :alter Foo mockuser 1) => falsey
    (authfn :add Bar mockuser 1) => falsey))

(fact "find-abilities in dir with 3 components"
  (let [abs (find-abilities 'cludje.demo.actions)]
    (count abs) => 2))
