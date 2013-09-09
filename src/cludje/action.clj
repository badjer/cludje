(ns cludje.action
  (:use cludje.system
        cludje.util))

(defmacro with-log-dsl [context & forms]
  `(let [~'log (partial log (?? ~context [:system :logger]))]
     ~@forms))

(defmacro with-datastore-dsl [context & forms]
  `(let [~'write (partial write (?? ~context [:system :data-store]))
         ~'query (partial query (?? ~context [:system :data-store]))
         ~'fetch (partial fetch (?? ~context [:system :data-store]))
         ~'delete (partial delete (?? ~context [:system :data-store]))
         ~'insert (partial insert (?? ~context [:system :data-store]))
         ~'save (partial save (?? ~context [:system :data-store]))]
     ~@forms))

(defmacro with-email-dsl [context & forms]
  `(let [~'send-mail (partial send-mail (?? ~context [:system :emailer]))]
    ~@forms))

(defmacro with-input-dsl [context & forms]
  `(let [~'input (?? ~context :input)
         ~'?in (partial ? (?? ~context :input))
         ~'??in (partial ?? (?? ~context :input))
         ~'&?in (partial &? (?? ~context :input))]
     ~@forms))

(defmacro with-output-dsl [context & forms]
  `(let [~'output #(assoc ~context :output %)]
     ~@forms))

(defmacro with-action-dsl [context & forms]
  `(with-log-dsl ~context
     (with-datastore-dsl ~context
       (with-email-dsl ~context
         (with-input-dsl ~context
           (with-output-dsl ~context
             ~@forms))))))
