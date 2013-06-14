(ns cludje.types)

(defprotocol IFieldType 
  (parse [self txt])
  (show [self x])
  (validate [self txt]))


(def Str 
  (reify IFieldType 
    (parse [self txt] txt) 
    (show [self x] x) 
    (validate [self txt] true)))

(def Email
  (reify IFieldType 
    (parse [self txt] txt) 
    (show [self x] x) 
    (validate [self txt] 
      (not (nil? (re-matches #"(?i)[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?" txt))))))

(def Password
  (reify IFieldType
    (parse [self txt] txt) 
    (show [self x] "********")
    (validate [self txt] (< 2 (.length txt)))))
