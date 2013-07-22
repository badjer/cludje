(ns cludje.auth-test
  (:use midje.sweet
        cludje.types
        cludje.testcontrollers
        cludje.core
        cludje.auth))

(defmodel Bar {:foo Str})

(fact "auth"
  (let [auth (make-auth mock-auth-fn)]
    (authorize auth :add :model {} {}) => truthy
    (authorize auth :add :model nil {}) => falsey))

(fact "find-abilities"
  (let [abs (find-abilities 'cludje.testcontrollers)
        authfn (apply make-auth-fn abs)]
    (count abs) => 2
    authfn => fn?
    ((first abs) :add Foo nil 1) => truthy
    (authfn :add Foo nil 1) => truthy
    (fact "second ability in controller also works"
      (authfn :remove Foo nil 1) => truthy)
    (authfn :alter Foo nil 1) => falsey
    (authfn :add Bar nil 1) => falsey))

(fact "find-abilities in dir with 3 components"
  (let [abs (find-abilities 'cludje.demo.actions)]
    (count abs) => 2))
