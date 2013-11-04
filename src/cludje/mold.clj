(ns cludje.mold
  (:require [clojure.string :as s])
  (:use cludje.types
        cludje.errors
        cludje.util))

(defprotocol IMold
  (fields [self])
  (field-names [self])
  ; TODO: Make this take context as an arg, so we can use fns as defaults
  (field-defaults [self])
  (required-fields [self])
  (invisible-fields [self])
  (computed-fields [self]))


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

(defn extend-ivalidateable [mold]
  (extend (type mold)
    IValidateable
    {:problems? problems?-})
  ; Return the original object so we can chain things
  mold)

(defn- show-computed- [mold input]
  (if (and (map? input) (not (empty? input)))
    (let [computed (computed-fields mold)]
      (merge input
             (into {} (for [[field fieldfn] computed]
                        [field (fieldfn input)]))))
    {}))

(defn- show- [mold input]
  (if (and (map? input) (not (empty? input)))
    (let [kees (concat (keys input) (keys (computed-fields mold)))
          fields (select-keys (fields mold) kees)
          output (show-computed- mold input)]
      (into {} (for [[field typ] fields] 
                 [field (show typ (get output field))])))
    {}))

(defn extend-ishowable [mold]
  (extend (type mold)
    IShowable
    {:show show-})
  ; Return the original object so we can chain things
  mold)

(defn- parse- [mold input]
  (let [defaults (field-defaults mold)]
    (into {}
          (for [[field typ] (fields mold)]
            [field (parse typ (get input field (get defaults field)))]))))

(defn extend-iparseable [mold]
  (extend (type mold)
    IParseable
    {:parse parse-})
  ; Return the original object so we can chain things
  mold)


(defn extend-imold [obj fs opts]
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
        computed (merge (when base (computed-fields base))
                        (get opts :computed))
        invisible (distinct (concat (when base (invisible-fields base))
                         (get opts :invisible [])))]
    (extend (type obj)
      IMold
      {:fields (fn [self] allfields)
       :field-names (fn [self] (map-vals fieldnames realize))
       :field-defaults (fn [self] (map-vals defaults realize))
       :required-fields (fn [self] reqfields)
       :computed-fields (fn [self] computed)
       :invisible-fields (fn [self] invisible)})
    ; Return the object so we can chain these calls
    obj))

(defn >Mold [fs opts]
  ; Terrible hack here.
  ; We want to create a new type at runtime. We can't just
  ; call (reify), it seems, because it gives back the same
  ; type every time.
  ; So instead, we eval in order to get new types
  ; NOTE: If specify gets added to Clojure, that's what we'd like here
  (let [obj (eval '(reify))]
    (-> obj
        (extend-imold fs opts)
        (extend-ivalidateable)
        (extend-ishowable)
        (extend-iparseable))))

(defn make [mold m]
  (if-let [problems (problems? mold m)]
    (throw-problems problems)
    (parse mold m)))

