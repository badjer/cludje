(ns cludje.modelstore-test
  (:use midje.sweet
        cludje.core
        cludje.types
        cludje.modelstore
        cludje.test))

(defmodel Cog {:amt Int})

(fact "ModelStore"
  (let [ms (->ModelStore 'cludje.modelstore-test)]
    (get-model- ms "Cog") => Cog
    (meta (get-model- ms "Cog")) => (has-keys :fields)
    (get-model- ms "Foobar") => nil))
