(ns cludje.datastore
  (:use cludje.system
        cludje.mold
        cludje.model)
  (:import java.io.File
           java.io.FileWriter
           java.util.UUID))


(def permitted-types
  "The types that are allowed to be in the database"
  #{Boolean Integer String Long Float Double clojure.lang.BigInt})

(defn- validate-db-data [data]
  (let [types (distinct (map type (vals data)))
        invalidtypes (set (remove permitted-types types))
        invalid-fields (filter #(invalidtypes (type (val %))) data)
        messages (for [field invalid-fields] (str (key field) " - " (type (val field))))]
    (if-not (empty? invalid-fields)
      (throw (Exception. (str "Invalid database type! The database cannot store these fields because the field types are unsupported. Please use a supported type for your data. " (clojure.string/join " " messages)))))))

(defn new-id []
  (str (UUID/randomUUID)))


(defrecord TestDatastore [dbatom]
  IDatastore
  (fetch [self coll kee] 
    (let [params {:_id kee}
          _ (validate-db-data params)
          tbl (tablename coll)
          results (query self tbl params)]
      (or (first results) nil)))
  (query [self coll params] 
    (let [_ (validate-db-data params)
          tbl (tablename coll)]
      (if (empty? params)
        (let [res (get @dbatom tbl)]
          (if (empty? res) nil res))
        (let [table (get @dbatom tbl)
              comparison-keys (keys params)
              row-matches? #(= params (select-keys % comparison-keys))
              res (filter row-matches? table)]
          (if (empty? res) nil res)))))
  (write [self coll kee data] 
    (let [kee (if kee kee (new-id)) ; If no kee is supplied, create a new one
          keemap {:_id kee}
          _ (validate-db-data keemap)
          _ (validate-db-data data)
          tbl (tablename coll)
          record (merge data keemap)
          oldtable (get @dbatom tbl)
          victims (set (query self tbl keemap))
          newtable (conj (remove victims oldtable) record)]
      (swap! dbatom assoc tbl newtable)
      kee))
  (delete [self coll kee] 
    (let [tbl (tablename coll)
          victims (set (query self tbl {:_id kee}))
          oldtable (get @dbatom tbl)
          newtable (remove victims oldtable)]
      (swap! dbatom assoc tbl newtable))))

(defn >TestDatastore 
  ([] (>TestDatastore {}))
  ([contents]
    (->TestDatastore (atom contents))))

(defn spit-testdatastore [db filename]
  (let [dba (:dbatom db)]
    (println "Saving db to " filename)
    (binding [*out* (FileWriter. filename)]
      (prn @dba))))

(defn slurp-testdatastore [filename]
  (println "Reading db from " filename)
  (when (.isFile (File. filename))
    (println "File found...")
    (when-let [dba (read-string (slurp filename))]
      (println "Contents were " dba)
      (>TestDatastore dba))))

(defn save [store model m]
  (let [parsed (make model m)
        kee (get m (keyname model))
        id (write store (tablename model) kee parsed)]
    {:_id id}))

(defn insert [store model m]
  (save store model (dissoc m (keyname model))))

