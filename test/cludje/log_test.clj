(ns cludje.log-test
  (:use midje.sweet
        cludje.system
        cludje.log))

(fact ">TestLogger"
  (let [logger (>TestLogger)]
    (log logger "hi") => anything
    @(:entries logger) => ["hi"]))

(fact ">ConsoleLogger"
  (let [logger (>ConsoleLogger)]
    (with-out-str (log logger "hi")) => "hi\n"))
