(ns cludje.action
  (:use cludje.system
        cludje.mold
        cludje.model
        cludje.util))

(defn save [context model m]
  (let [store (? context [:system :data-store])
        parsed (make model m)
        kee (get m (keyname model))
        id (write store (tablename model) kee parsed)]
    {:_id id}))

(defn insert [context model m]
  (save context model (dissoc m (keyname model))))
