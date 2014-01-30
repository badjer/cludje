(ns cludje.authorize-test
  (:use midje.sweet
        cludje.test
        cludje.types
        cludje.system
        cludje.authorize
        cludje.datastore
        cludje.model
        cludje.mold))

(defn- >input 
  ([action-sym] (>input action-sym {}))
  ([action-sym input] {:input (assoc input :_action action-sym) :user {:username "a"}}))

(def Cog (>Model {:amt Int} {:modelname "cog"}))

(def ab-all-cog (>Ability :add Cog true))

(facts "ability with no filter"
  (ab-all-cog (>input 'add-cog)) => truthy
  (ab-all-cog (>input 'remove-cog)) => falsey)

(facts "ability works with fully-qualified action-sym"
  (ab-all-cog (>input `add-cog)) => truthy
  (ab-all-cog (>input :cludje.authorize-test/add-cog)) => truthy
  (ab-all-cog (>input `remove-cog)) => falsey
  (ab-all-cog (>input :cludje.authorize-test/remove-cog)) => falsey)

(def Foo (>Model {:amt Int} {:modelname "cog" :tablename "gear"}))
(def ab-model-cog (>Ability :add Foo true))

(facts "ability matches on modelname if it's different"
  (ab-model-cog (>input 'add-cog)) => truthy
  (ab-model-cog (>input 'remove-cog)) => falsey)

(def ab-cog (>Ability
  :add Cog #(= 1 (:amt (:input %)))))

(facts "ability with fn"
  (ab-cog (>input 'add-cog {:amt 1})) => truthy
  (ab-cog (>input 'add-cog {:amt 2})) => falsey
  (ab-cog (>input 'remove-cog {:amt 1})) => falsey
  (ab-cog (>input 'add-cog {})) => falsey)

(facts "ability should work with strs/keywords in do-action"
  (ab-cog (>input "add-cog" {:amt 1})) => truthy
  (ab-cog (>input :add-cog {:amt 1})) => truthy
  (ab-cog (>input "add-cog" {:amt 2})) => falsey
  (ab-cog (>input :add-cog {:amt 2})) => falsey
  (ab-cog (>input "remove-cog" {:amt 1})) => falsey
  (ab-cog (>input :remove-cog {:amt 1})) => falsey)

(def ab-str-model (>Ability :add "foo" true))

(facts "defability works with strs in ability"
  (ab-str-model (>input 'add-foo)) => truthy
  (ab-str-model (>input 'remove-foo)) => falsey)
  
(def ab-star-cog (>Ability :* Cog true))

(facts "ability with *"
  (ab-star-cog (>input :add-cog)) => truthy
  (ab-star-cog (>input :remove-cog)) => truthy
  (ab-star-cog (>input :add-foo)) => falsey)

(def ab-star-override-cog
  (>Ability
    :delete Cog false
    :* Cog true))

(facts "ability earlier entry overrides later one"
  (ab-star-override-cog (>input 'add-cog)) => truthy
  (ab-star-override-cog (>input 'delete-cog)) => falsey)

(def Person (>Model {:name Str} {:modelname "person"}))
(def ab-cog-person
  (>Ability
    :add Cog #(= 1 (:amt (:input %)))
    :add Person #(= (:name (:input %)) (:username (:user %)))))

(facts "ability with multiple permissions"
  (ab-cog-person (>input 'add-cog {:amt 1})) => truthy
  (ab-cog-person (>input 'add-cog {:amt 2})) => falsey
  (ab-cog-person (>input 'foo-cog {:amt 1})) => falsey
  (ab-cog-person (>input 'add-person {:name "a"})) => truthy
  (ab-cog-person (>input 'add-person {:name "b"})) => falsey
  (ab-cog-person (>input 'foo-person {:name "a"})) => falsey)

(def ab-ac-vector
  (>Ability
    [:add :delete] Cog true))

(facts "ability with a vector of actions"
  (ab-ac-vector (>input 'add-cog)) => truthy
  (ab-ac-vector (>input 'delete-cog)) => truthy
  (ab-ac-vector (>input 'foo-cog)) => falsey
  (ab-ac-vector (>input 'add-person)) => falsey)

(def ab-cog-anon
  (>Ability
    :add Cog :anon))

(facts "ability only allows anonymous access for :anon"
  (ab-cog-anon (dissoc (>input 'add-cog) :user)) => truthy
  (ab-all-cog (dissoc (>input 'add-cog) :user)) => (throws-401))


(facts "AbilityAuthorizer"
  (let [auth (>AbilityAuthorizer ab-all-cog)]
    (fact "implements IAuthorizer"
      (satisfies? IAuthorizer auth) => true)
    (fact "abilities work"
      (can? auth (>input 'add-cog)) => truthy
      (can? auth (>input 'foo-cog)) => falsey
      (can? auth (>input 'add-person)) => falsey))
  (let [multauth (>AbilityAuthorizer ab-all-cog ab-cog-person)]
    (fact "works with multiple abilities"
      (can? multauth (>input 'add-cog)) => truthy
      (can? multauth (>input 'add-person {:name "a"})) => truthy)))

