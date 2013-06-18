(ns cludje.database
  (:use cludje.core))

(def permitted-types
  "The types that are allowed to be in the database"
  #{Boolean Integer String Long Float Double clojure.lang.PersistentHashSet clojure.lang.Keyword clojure.lang.PersistentArrayMap})

(defn- validate-db-data [data]
  (let [types (distinct (map type (vals data)))
        invalidtypes (set (remove permitted-types types))
        invalid-fields (filter #(invalidtypes (type (val %))) data)
        messages (for [field invalid-fields] (str (key field) " - " (type (val field))))]
    (if-not (empty? invalid-fields)
      (throw (Exception. (str "Invalid database type! The database cannot store these fields because the field types are unsupported. Please use a supported type for your data. " (clojure.string/join " " messages)))))))

(defn new-id []
  (str (java.util.UUID/randomUUID)))


(defrecord MemDb [dbatom]
  IDatabase
  (fetch- [self coll kee] 
    (let [params {:_id kee}
          _ (validate-db-data params)
          results (query- self coll params)]
      (or (first results) nil)))
  (query- [self coll params] 
    (let [_ (validate-db-data params)
          kwcoll (keyword coll)]
      (if (empty? params)
        (get @dbatom kwcoll)
        (let [table (get @dbatom kwcoll)
              comparison-keys (keys params)
              row-matches? #(= params (select-keys % comparison-keys))]
          (filter row-matches? table)))))
  (write- [self coll kee data] 
    (let [kee (if kee kee (new-id)) ; If no kee is supplied, create a new one
          keemap {:_id kee}
          _ (validate-db-data keemap)
          _ (validate-db-data data)
          kwcoll (keyword coll)
          record (merge data keemap)
          oldtable (get @dbatom kwcoll)
          victims (set (query- self kwcoll keemap))
          newtable (conj (remove victims oldtable) record)]
      (swap! dbatom assoc kwcoll newtable)
      kee))
  (delete- [self coll kee] 
    (let [kwcoll (keyword coll)
          victims (set (query- self kwcoll {:_id kee}))
          oldtable (get @dbatom kwcoll)
          newtable (remove victims oldtable)]
      (swap! dbatom assoc kwcoll newtable))))

