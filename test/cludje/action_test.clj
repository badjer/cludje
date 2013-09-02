(ns cludje.action-test
  (:use midje.sweet
        cludje.test
        cludje.util
        cludje.types
        cludje.model
        cludje.action
        cludje.log
        cludje.system
        cludje.email
        cludje.datastore))

(defn >log-context [logger]
  {:system {:logger logger}})

(fact "with-log-dsl"
  (let [logger (>TestLogger)
        context (>log-context logger)]
    (with-log-dsl context
      (fact "defines log"
        (log "hi") =not=> (throws)
        @(? logger :entries) => ["hi"]))))

(def row {:a 1})

(def Cog (>Model "cog" {:a Int} {}))

(defn >ds-context [datastore]
  {:system {:data-store datastore}})

(fact "with-datastore-dsl"
  (let [ds (>TestDatastore)
        context (>ds-context ds)]
    (with-datastore-dsl context
      (fact "defines write"
        (write :cog 1 row) =not=> (throws)
        (first (query :cog {})) => (contains row))
      (fact "defines query"
        (count (query :cog {})) => 1)
      (fact "defines fetch"
        (fetch :cog 1) => (contains row))
      (fact "defines delete"
        (delete :cog 1) => anything
        (count (query :cog {})) => 0)
      (fact "defines insert"
        (insert Cog row) => anything
        (count (query :cog {})) => 1)
      (fact "defines save"
        (save Cog row) => anything
        (count (query :cog {})) => 2))))

(def message {:to "a@b.cd" :from "b@b.cd" :subject "s" :text "b" :body "h"})
(defn >email-context [emailer]
  {:system {:emailer emailer}})

(fact "with-email-dsl"
  (let [emailer (>TestEmailer)
        context (>email-context emailer)]
    (with-email-dsl context
      (fact "defines send-mail"
        (send-mail message) => anything
        @(? emailer :messages) => [message]))))

(def in-context {:input {:a 1 :b 2}})

(fact "with-input-dsl"
  (with-input-dsl in-context
    (fact "defines ?in"
      (?in :a) => 1)
    (fact "defines ??in"
      (??in :z) => nil)
    (fact "defines &?in"
      (&?in :z :a) => 1)))

