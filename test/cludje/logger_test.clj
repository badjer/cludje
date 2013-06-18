(ns cludje.logger-test
  (:use cludje.core
        cludje.logger
        midje.sweet))


(fact "consolelogger"
  (let [logger (->ConsoleLogger)]
    (with-out-str (log- logger "hi")) => "hi\n"))

