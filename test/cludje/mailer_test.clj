(ns cludje.mailer-test
  (:use midje.sweet
        cludje.core
        cludje.logger
        cludje.mailer))

(def mail {:to "a@b.cd" :from "b@b.cd" :subject "test"
           :body "hi" :text "hi"})

(fact "memmailer"
  (let [mailatom (atom [])
        mailer (->MemMailer mailatom)]
    (send-mail- mailer mail) => anything
    (count @mailatom) => 1
    (first @mailatom) => mail))

(fact "logmailer"
  (let [mailer (->LogMailer (->ConsoleLogger))]
    (with-out-str (send-mail- mailer mail)) => (with-out-str (println mail))))

