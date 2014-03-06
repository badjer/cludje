(ns cludje.mold
  (:require [clojure.string :as s])
  (:use cludje.types
        cludje.errors
        cludje.util))

(defprotocol IMold
  (fields [self])
  (field-names [self])
  (field-defaults [self])
  (required-fields [self])
  (invisible-fields [self]))


(defn friendly-name 
  ([mold field]
   (if (satisfies? IMold mold)
     (get (field-names mold) field)
     (friendly-name field)))
  ([field] (s/capitalize (name field))))


(defn needs [data & kees]
  "Generate an error if any of the supplied keys is missing from data"
  (apply merge
         (for [kee kees]
           (if-not (value? (kee data)) 
             {kee (str "Please supply a value for " (friendly-name kee))}))))

(defn- problems?- [mold input]
  (let [res (merge (apply needs input (required-fields mold)) 
                   (into {} (for [[field typ] (fields mold)] 
                              (let [errs (problems? typ (get input field))]
                                (when errs
                                  [field errs])))))]
    (when-not (empty? res) res)))

(defn- special-field? [kee]
  (->> kee
       (name)
       (re-find #"^__")))

(defn- show- [mold input]
  (if (and (map? input) (not (empty? input)))
    (let [specials (filter special-field? (keys input))
          fs (select-keys (fields mold) (keys input))
          included (concat specials (keys fs))]
      (into {} (for [kee included]
                 [kee (show (get fs kee Anything) (get input kee))])))
    {}))

(defn- parse- [mold input]
  (let [defaults (field-defaults mold)]
    (into {}
          (for [[field typ] (fields mold)]
            [field (parse typ (get input field (get defaults field)))]))))

(defn extend-iuitype [clss]
  (extend clss
    IUIType
    {:parse parse- 
     :show show- 
     :problems? problems?-})
  ; Return the original object so we can chain things
  clss)


(defn extend-imold [clss fs opts]
  (let [base (get fs :_)
        allfields (merge 
                    (when base (fields base))
                    (dissoc fs :_))
        ; Required fields are all fields, unless specified otherwise
        reqfields (distinct (concat 
                    (when base (required-fields base))
                    (get opts :required (vec (keys allfields)))))
        fieldnames (merge 
                     (when base (field-names base))
                     (zipmap (keys allfields) 
                             (map friendly-name (keys allfields)))
                     (get opts :names {}))
        defaults (merge (when base (field-defaults base))
                        (get opts :defaults))
        invisible (distinct (concat (when base (invisible-fields base))
                         (get opts :invisible [])))]
    (extend clss
      IMold
      {:fields (fn [self] allfields)
       :field-names (fn [self] (map-vals fieldnames realize))
       :field-defaults (fn [self] (map-vals defaults realize))
       :required-fields (fn [self] reqfields)
       :invisible-fields (fn [self] invisible)})
    ; Return the object so we can chain these calls
    clss))

(defmacro defmold [nam fs & [opts]]
    (let [opts (or opts {})
          classname (symbol (str nam "-type"))
          constructor (symbol (str "->" nam "-type"))
          instance (symbol nam)]
      `(do 
         (deftype ~classname [])
         (extend-imold ~classname ~fs ~opts)
         (extend-iuitype ~classname)
         (def ~instance (~constructor)))))


(defn >Mold 
  ([fs]
   (>Mold fs {}))
  ([fs opts]
    ; Terrible hack here.
    ; We want to create a new type at runtime. We can't just
    ; call (reify), it seems, because it gives back the same
    ; type every time.
    ; So instead, we eval in order to get new types
    ; NOTE: If specify gets added to Clojure, that's what we'd like here
    (let [obj (eval '(reify))]
      (-> (type obj)
          (extend-imold fs opts)
          (extend-iuitype))
      obj)))

(defn make [mold m]
  (if-let [problems (problems? mold m)]
    (throw-problems problems)
    (parse mold m)))

