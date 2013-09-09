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

(def logger (>TestLogger))
(def log-context {:system {:logger logger}})

(fact "with-log-dsl"
  (with-log-dsl log-context
    (fact "defines log"
      (log "hi") =not=> (throws)
      @(? logger :entries) => ["hi"])))

(def row {:a 1})

(def Cog (>Model {:a Int} {:modelname "cog"}))

(def datastore (>TestDatastore))
(def ds-context {:system {:data-store datastore}})

(fact "with-datastore-dsl"
  (with-datastore-dsl ds-context
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
      (count (query :cog {})) => 2)))

(def message {:to "a@b.cd" :from "b@b.cd" :subject "s" :text "b" :body "h"})
(def emailer (>TestEmailer))
(def email-context {:system {:emailer emailer}})

(fact "with-email-dsl"
  (with-email-dsl email-context
    (fact "defines send-mail"
      (send-mail message) => anything
      @(? emailer :messages) => [message])))

(def in-context {:input {:a 1 :b 2}})

(fact "with-input-dsl"
  (with-input-dsl in-context
    (fact "defines ?in"
      (?in :a) => 1)
    (fact "defines ??in"
      (??in :z) => nil)
    (fact "defines &?in"
      (&?in :z :a) => 1)))

(fact "with-output-dsl"
  (with-output-dsl in-context
    (output {:z 1}) => (contains {:output {:z 1}})
    (output {:z 1}) => (contains in-context)))

(def action-context 
  (-> (merge in-context email-context)
      (assoc-in [:system :data-store] datastore)
      (assoc-in [:system :logger] logger)))

(fact "with-action-dsl"
  (with-action-dsl action-context
    (fact "defines a log fn"
      (log "asdf") =not=> (throws))
    (fact "defines a datastore fn"
      (query :cog {}) =not=> (throws))
    (fact "defines an email fn"
      (send-mail message) =not=> (throws))
    (fact "defines an input fn"
      (?in :a) =not=> (throws))
    (fact "defines input"
      input => {:a 1 :b 2})
    (fact "defines output"
      output =not=> (throws))
    ;(fact "defines with-lookup"
      ;(with-lookup {} Cog) =not=> (throws))
    ))

