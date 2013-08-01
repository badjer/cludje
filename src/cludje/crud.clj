(ns cludje.crud
  (:use cludje.core
        cludje.types))

(def crud-actions [:new :add :show :edit :alter :delete :list])

(defaction default-global-dologin
  (login input))
(defaction default-global-login
  {})
(defaction default-global-logout
  (logout input))
(defaction default-global-data
  {:user (current-user)
   :menu [{:text "A" :url "/a"} {:text "B" :url "/b"}]
   :footer "© 2013 Cludje"
   :title "Cludje"})

(defmacro def-system-actions []
  `(do
     (defability ~'-system-data-ability
       :* "Global" :anon)
     (defaction ~'global-dologin
       (default-global-dologin ~'system ~'input))
     (defaction ~'global-login
       (default-global-login ~'system ~'input))
     (defaction ~'global-logout
       (default-global-logout ~'system ~'input))
     (defaction ~'global-data
       (default-global-data ~'system ~'input))))

(defn realize-one [system input v]
  "Try to get a final-value for v.
  If it's a var, deref it. If it's a fn, call it with [system input]"
  (cond
    (fn? v) (v system input)
    (var? v) (realize-one system input @v)
    :else v))

(defn realize-map [system input m]
  (into {} (for [[k v] m] [k (realize-one system input v)])))


(defn crud-model-list [model system input]
  (let [listkee (keyword (str (table-name model) "s"))
         parts (partitions model)
         defs (defaults model)
         ; This is a bit complicated because of optimization
         ; We want parts-def to be (select-keys (realize-map defs)),
         ; but we do an extra internal select-keys inside of 
         ; realize-map in order to avoid evaling any default
         ; fields that we don't need
         ; The reason we add a (make model..) onto this is because
         ; we want to handle the case where the default is of the 
         ; wrong type - this will convert it
         part-defs (select-keys 
                      (make model
                            (realize-map system input
                                         (select-keys defs parts)))
                      parts)
         ; We want to get a parsed version of any of the partitions
         ; that the user passed as input - that's part-inp
         parsed (make model input)
         supplied-parts (clojure.set/intersection 
                           (set parts) 
                           (set (keys input)))
         part-inp (select-keys parsed supplied-parts)
         ; The query-paras are the defaults and input of the partitions
         query-paras (merge part-defs part-inp)
        rows (query system model query-paras)
        shown (map (partial show model) rows)]
    {listkee shown}))
     ;{listkee (query system model query-paras)}))

(defn crud-model-new [model system input]
  (realize-map system input (defaults model)))

(defn crud-model-add [model system input]
  (let [defs (make model (realize-map system input (defaults model)))
        combined (merge defs input)]
    (-> (insert system model combined)
        (with-alert :success "Saved"))))

(defn crud-model-show [model system input]
  (->> model
       (key-name)
       (? input)
       (fetch system model)
       (show model)))

(defn crud-model-edit [model system input]
  (fetch system model (? input (key-name model))))

(defn crud-model-alter [model system input]
  (? input (key-name model))
  (-> (save system model input)
      (with-alert :success "Saved")))

(defn crud-model-delete [model system input]
  (delete system model (? input (key-name model)))
  nil)


(defmacro def-crud-actions [model-sym]
  (let [model @(resolve model-sym)
        modelname (table-name model)]
    `(do
       (defaction ~(symbol (str modelname "-list"))
         (crud-model-list ~model-sym ~'system ~'input))
       (defaction ~(symbol (str modelname "-new"))
         (crud-model-new ~model-sym ~'system ~'input))
       (defaction ~(symbol (str modelname "-add"))
         (crud-model-add ~model-sym ~'system ~'input))
       (defaction ~(symbol (str modelname "-show"))
         (crud-model-show ~model-sym ~'system ~'input))
       (defaction ~(symbol (str modelname "-edit"))
         (crud-model-edit ~model-sym ~'system ~'input))
       (defaction ~(symbol (str modelname "-alter"))
         (crud-model-alter ~model-sym ~'system ~'input))
       (defaction ~(symbol (str modelname "-delete"))
         (crud-model-delete ~model-sym ~'system ~'input)))))

(defn with-lookup- [m model system input]
  (let [action-name (str (table-name model) "-list")
        action (resolve-action system {:_action action-name})
        lookup-res (run-action system action {})]
    (merge lookup-res m)))

(defmacro with-lookup [m model]
  `(with-lookup- ~m ~model ~'system ~'input))
