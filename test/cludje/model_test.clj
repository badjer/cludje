(ns cludje.model-test
  (:use midje.sweet
        cludje.test
        cludje.types
        cludje.mold
        cludje.model))

(def fs {:name Str :price Money})

(def Cogmodel (>Model fs {:modelname "cog"}))

(defmodel Carmodel fs :modelname "carses" 
                      :defaults {:price 10000} 
                      :tablename "cars_tbl")
(defmodel Jeepmodel fs)

(fact "defmodel"
  (fact "with args"
    (fact "sets fields"
      (fields Carmodel) => (assoc fs :_id Str))
    (fact "sets mold options"
      (field-defaults Carmodel) => {:price 10000})
    (fact "sets model options"
      (modelname Carmodel) => "carses"))
  (fact "no args"
    (fact "sets fields"
      (fields Jeepmodel) => (assoc fs :_id Str))
    (fact "sets mold options"
      (field-defaults Jeepmodel) => {})
    (fact "defaults modelname to name"
      (modelname Jeepmodel) => "jeepmodel")
  ))

(fact ">Model"
  (fact "implements IModel"
    (fact "can build a model"
      Cogmodel => (partial satisfies? IModel))
    (fact "is a mold"
      Cogmodel => (partial satisfies? IMold)
      (fact "adds kee field"
        (fields Cogmodel) => (has-keys :_id)
        (fact "unless no-key is supplied"
          (fields (>Model fs {:modelname :cog :no-key true})) =not=> (has-keys :_id)))
      (fact "required-fields excludes kee"
        (required-fields Cogmodel) =not=> (contains :_id))
      (fact "invisible-fields includes kee"
        (invisible-fields Cogmodel) => (contains :_id)))
    (fact "modelname"
      (fact "with string model"
        (modelname (>Model {} {:modelname "cogi"})) => "cogi")
      (fact "with keyword model"
        (modelname (>Model {} {:modelname :cogi})) => "cogi"))
    (fact "tablename"
      (fact "with string table"
        (tablename (>Model {} {:modelname "cog"})) => "cog"
        (tablename (>Model {} {:modelname "Cog"})) => "cog")
      (fact "with keyword table"
        (tablename (>Model {} {:modelname :cog})) => "cog"
        (tablename (>Model {} {:modelname :Cog})) => "cog")
      (fact "is set automatically from modelname"
        (tablename (>Model {} {:modelname :Cog})) => "cog")
      (fact "can be different than modelname"
        (tablename (>Model {} {:modelname :Cog :tablename "cogt"})) => "cogt"))
    (fact "keyname"
      (keyname Cogmodel) => :_id)
    ))

(defmodel Gear {:name Str :teeth Int :size Int} 
  :modelname "gear" :required [:name] :invisible [:teeth])

(fact ">Model overrides mold fields"
  (fact "required-fields"
    (required-fields Gear) => [:name])
  (fact "invisible-fields"
    (invisible-fields Gear) => [:teeth :_id]))
