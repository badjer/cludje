(ns cludje.action
  (:use cludje.system
        cludje.mold
        cludje.model
        cludje.util))

(defn save [context model m]
  (let [store (? context [:system :data-store])
        parsed (make model m)
        kee nil;(get-key model parsed)
        id nil];(write2 sys model kee parsed)]
    {:_id id}))

(defn insert [context model m]
  );(save sys model (dissoc m (key-name model))))



