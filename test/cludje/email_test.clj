(ns cludje.email-test
  (:use midje.sweet
        cludje.test
        cludje.system
        cludje.email))

(def mail {:to "a@b.cd" :from "b@b.cd" :subject "test"
           :body "hi" :text "hi"})

(fact "TestEmailer"
  (let [mailatom (atom [])
        mailer (->TestEmailer mailatom)]
    (send-mailmessage mailer mail) => anything
    (count @mailatom) => 1
    (first @mailatom) => mail))

;(fact "LogEmailer"
  ;(let [mailer (->LogEmailer (->ConsoleLogger))]
    ;(with-out-str (send-mail- mailer mail)) => (with-out-str (println mail))))

