(ns cludje.action
  (:use cludje.system
        cludje.mold
        cludje.util))

(defmacro with-log-dsl [request & forms]
  `(let [~'log (partial log (?? ~request [:system :logger]))]
     ~@forms))

(defmacro with-datastore-dsl [request & forms]
  `(let [~'write (partial write (?? ~request [:system :data-store]))
         ~'query (partial query (?? ~request [:system :data-store]))
         ~'fetch (partial fetch (?? ~request [:system :data-store]))
         ~'delete (partial delete (?? ~request [:system :data-store]))
         ~'insert (partial insert (?? ~request [:system :data-store]))
         ~'save (partial save (?? ~request [:system :data-store]))]
     ~@forms))

(defmacro with-email-dsl [request & forms]
  `(let [~'send-mail (partial send-mail (?? ~request [:system :emailer]))]
    ~@forms))

(defmacro with-input-dsl [request & forms]
  `(let [~'input (?? ~request :input)
         ~'?in (partial ? (?? ~request :input))
         ~'??in (partial ?? (?? ~request :input))
         ~'&?in (partial &? (?? ~request :input))
         ~'with-optional-param-in (fn 
                                    ([kee# typ#] 
                                     (with-optional-param (?? ~request :input) kee# typ#))
                                    ([m# kee# typ#] 
                                     (with-optional-param m# (?? ~request :input) kee# typ#)))
         ~'with-param-in (fn 
                           ([kee# default# typ#]
                            (with-param (?? ~request :input) kee# default# typ#))
                           ([m# kee# default# typ#]
                            (with-param m# (?? ~request :input) kee# default# typ#)))

         ]
     ~@forms))

(defmacro with-output-dsl [request & forms]
  `(let [~'output #(assoc ~request :output %)]
     ~@forms))

(defmacro with-action-dsl [request & forms]
  `(with-log-dsl ~request
     (with-datastore-dsl ~request
       (with-email-dsl ~request
         (with-input-dsl ~request
           (with-output-dsl ~request
             ~@forms))))))
