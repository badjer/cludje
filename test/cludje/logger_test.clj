(ns cludje.logger-test
  (:use cludje.core
        cludje.logger
        midje.sweet))

(fact "memlogger"
  (let [logatom (atom [])
        logger (->MemLogger logatom)]
    (log- logger "hi") => anything
    (count @logatom) => 1
    (first @logatom) => "hi"))

(fact "consolelogger"
  (let [logger (->ConsoleLogger)]
    (with-out-str (log- logger "hi")) => "hi\n"))

