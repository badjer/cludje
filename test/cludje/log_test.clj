(ns cludje.log-test
  (:use midje.sweet
        cludje.test
        cludje.system
        cludje.email
        cludje.log))

(fact ">TestLogger"
  (let [logger (>TestLogger)]
    (log logger "hi") => anything
    @(:entries logger) => ["hi"]))

(fact ">ConsoleLogger"
  (let [logger (>ConsoleLogger)]
    (with-out-str (log logger "hi")) => "hi\n"))

(fact ">MailLogger"
  (let [mailer (>TestEmailer)
        logger (>MailLogger mailer "a@b.cd" "b@b.cd" "Err")]
    (log logger "hi") => anything
    @(:messages mailer) => (just-item? {:from "a@b.cd" :to "b@b.cd" :subject "Err" :text "hi"})))

(fact ">CompositeLogger"
  (let [l1 (>TestLogger)
        l2 (>TestLogger)
        logger (>CompositeLogger l1 l2)]
    (log logger "hi") => anything
    @(:entries l1) => ["hi"]
    @(:entries l2) => ["hi"]))
