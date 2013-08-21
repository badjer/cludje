(ns cludje.model-test
  (:use midje.sweet
        cludje.test
        cludje.types
        cludje.mold
        cludje.model))

(def fs {:name Str :price Money})

(def Cogmodel (->Model :cog fs {}))

(fact "->Model"
  (fact "implements IModel"
    (fact "can build a model"
      Cogmodel => (partial satisfies? IModel))
    (fact "mold"
      (fact "should return mold"
        (mold Cogmodel) => (partial satisfies? IMold))
      (fact "adds kee field"
        (fields (mold Cogmodel)) => (has-keys :_id)
        (fact "unless no-key is supplied"
          (fields (mold (->Model :cog fs {:no-key true}))) =not=> (has-keys :_id)))
      (fact "required-fields excludes kee"
        (required-fields (mold Cogmodel)) =not=> (contains :_id))
      (fact "invisible-fields includes kee"
        (invisible-fields (mold Cogmodel)) => (contains :_id)))
    (fact "tablename"
      (fact "with string table"
        (tablename (->Model "cog" {} {})) => "cog"
        (tablename (->Model "Cog" {} {})) => "cog")
      (fact "with keyword table"
        (tablename (->Model :cog {} {})) => "cog"
        (tablename (->Model :Cog {} {})) => "cog"))
    (fact "keyname"
      (keyname Cogmodel) => :_id)
    (fact "partitions"
      (fact "supplied"
        (partitions (->Model :cog fs {:partitions [:name]})) => [:name])
      (fact "not supplied"
        (partitions (->Model :cog fs {})) => []))))

