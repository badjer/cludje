(ns cludje.mold
  (:require [clojure.string :as s])
  (:use cludje.types
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

(defn extend-ivalidateable [mold]
  (extend (type mold)
    IValidateable
    {:problems? problems?-})
  ; Return the original object so we can chain things
  mold)

(defn- show- [mold input]
  (when (and (map? input) (not (empty? input)))
    (let [kees (keys input) 
          fields (select-keys (fields mold) kees)] 
      (into {} (for [[field typ] fields] 
                 [field (show typ (get input field))])))))

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

(defn >Mold [fs opts]
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
    (-> (reify IMold
          (fields [self] allfields)
          (field-names [self] (map-vals fieldnames realize))
          (field-defaults [self] (map-vals defaults realize))
          (required-fields [self] reqfields)
          (invisible-fields [self] invisible))
        (extend-ivalidateable)
        (extend-ishowable)
        (extend-iparseable)
        )))

