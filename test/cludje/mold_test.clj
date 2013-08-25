(ns cludje.mold-test
  (:use cludje.mold
        cludje.util
        cludje.test
        cludje.types
        midje.sweet))

(defn getx [] "x")

(def fs {:name Str :price Money})

(def Cog (>Mold fs {:defaults {:price 42}}))
(def Bike (>Mold {:front Cog :back Cog :gears Int} {}))


(fact ">Mold"
  (fact "implements IMold"
    (fact "Can create a mold"
      (let [m (>Mold fs {})]
        (satisfies? IMold m) => true))

    (fact "fields"
      (let [m (>Mold fs {})]
        (fields m) => fs))

    (fact "field-names"
      (let [m (>Mold fs {:names {:name "MyName"}})]
        (field-names m) => {:name "MyName" :price "Price"})
      (fact "with a fn"
        (let [m (>Mold fs {:names {:name getx}})]
          (field-names m) => (contains {:name "x"}))))

    (fact "field-defaults"
      (let [m (>Mold fs {:defaults {:price 42}})]
        (field-defaults m) => {:price 42})
      (fact "with a fn"
        (let [m (>Mold fs {:defaults {:name getx}})]
          (field-defaults m) => {:name "x"})))

    (fact "required-fields"
      (let [m (>Mold fs {:required [:name]})]
        (required-fields m) => [:name]))

    (fact "invisible-fields"
      (let [m (>Mold fs {:invisible [:price]})]
        (invisible-fields m) => [:price]))))

(fact ">Mold implements IValidateable"
  (let [m (>Mold fs {})]
    (satisfies? IValidateable m) => true
    (problems? m {}) => (just-keys :name :price)
    (problems? m {:name "a" :price "asdf"}) => (just-keys :price)
    (problems? m {:name "a" :price 123}) => falsey

    (fact "ignores extra fields"
      (problems? m {:name "a" :price 123 :foo 1}) => falsey)

    (fact "with nested mold"
      (problems? Bike {:front {:name "a" :price 123}
                       :back {:name "a" :price 234}
                       :gears "asdf"}) => (just-keys :gears)
      (problems? Bike {:front {:name "a" :price "asdf"}
                       :back {:name "a" :price 234}
                       :gears 1}) => (just-keys :front))))

(fact ">Mold implements IShowable"
  (let [m (>Mold fs {})]
    (satisfies? IShowable m) => true
    (show m {:price 123}) => {:price "$1.23"}

    (fact "with nested mold"
      (show Bike {:front {:name "a" :price 1}
                  :back {:name "b" :price 2}
                  :gears 3}) =>
      {:front {:name "a" :price "$0.01"}
       :back {:name "b" :price "$0.02"}
       :gears "3"})

    (fact "hides extra fields"
      (show m {:name "a" :price 1 :foo 1}) => (just-keys :name :price)
      (fact "with nested mold"
        (show Bike {:front {:name "a" :price 1 :foo 1}
                    :back {:name "b" :price 2}
                    :gears 3 :bar 2}) =>
        {:front {:name "a" :price "$0.01"}
         :back {:name "b" :price "$0.02"}
         :gears "3"}))))

(fact ">Mold implements IParseable"
  (let [m (>Mold fs {})]
    (satisfies? IParseable m) => true

    (fact "parse returns all fields"
      (parse m {}) => (has-keys :name :price)
      (parse m {:name "A"}) => {:name "A" :price nil}
      (parse m {:name "A" :price 1}) => {:name "A" :price 1})

    (fact "with nested mold"
      (parse Bike {:front {:name "a" :price 1}
                   :back {:name "b" :price 2}
                   :gears 1}) =>
      {:front {:name "a" :price 1}
       :back {:name "b" :price 2}
       :gears 1})

    (fact "removes extra fields"
      (parse m {:name "A" :price 1 :foo 1}) => (just-keys :name :price)
      (fact "with nested mold"
        (parse Bike {:front {:name "a" :price 1 :foo 1}
                     :back {:name "b" :price 2}
                     :gears 2 :bar 4}) =>
        {:front {:name "a" :price 1}
         :back {:name "b" :price 2}
         :gears 2}))

    (fact "with defaults"
      (fact "value"
        (let [md (>Mold fs {:defaults {:price 42}})]
          (fact "if not supplied"
            (parse md {}) => (contains {:price 42}))
          (fact "if supplied"
            (parse md {:price 11}) => (contains {:price 11}))))

      (fact "fn"
        (let [mf (>Mold fs {:defaults {:name getx}})]
          (fact "if not supplied"
            (parse mf {}) => (contains {:name "x"}))
          (fact "if supplied"
            (parse mf {:name "a"}) => (contains {:name "a"})))))))



(def Cog+ (>Mold {:_ Cog :id Int} {}))

(fact "can extend an existing mold"
  (fields Cog+) => (has-keys :name :price :id)
  (field-names Cog+) => (has-keys :name :price :id)
  (field-defaults Cog+) => (has-keys :price)
  (required-fields Cog+) => [:price :name :id]
  (invisible-fields Cog+) => [])


(def Cog-list (>Mold {:cogs (list-of Cog)} {}))

(fact "can have a list in a mold"
  (parse Cog-list {:cogs [{:name "a" :price 1}
                          {:name "b" :price 2}]}) =>
  {:cogs [{:name "a" :price 1} {:name "b" :price 2}]})
