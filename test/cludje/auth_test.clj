(ns cludje.auth-test
  (:use midje.sweet
        cludje.types
        cludje.testcontrollers
        cludje.core
        cludje.auth))

(defmodel Bar {:foo Str})

(fact "auth"
  (let [auth (make-auth mock-auth-fn)]
    (authorize auth :action :model {} {}) => truthy
    (authorize auth :action :model nil {}) => falsey))

(fact "find-abilities"
  (let [abs (find-abilities 'cludje.testcontrollers)
        authfn (apply make-auth-fn abs)]
    (count abs) => 1
    authfn => fn?
    ((first abs) :add Foo nil 1) => truthy
    (authfn :add Foo nil 1) => truthy
    (authfn :delete Foo nil 1) => falsey
    (authfn :add Bar nil 1) => falsey))
