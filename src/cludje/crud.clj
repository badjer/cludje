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

(defn make-crud-model-list [model]
  `(let [listkee# (keyword (str (table-name ~model) "s"))
         parts# (partitions ~model)
         defs# (defaults ~model)
         part-defs# (realize-map ~'system ~'input 
                                 (select-keys defs# parts#))
         query-paras# (merge part-defs# (select-keys ~'input parts#))]
     {listkee# (~'query ~model query-paras#)}));))

(defn make-crud-model-new [model]
  `(realize-map ~'system ~'input (defaults ~model)))

(defn make-crud-model-add [model]
  `(-> (~'insert ~model ~'input)
       (with-alert :success "Saved")))

(defn make-crud-model-show [model]
  `(~'fetch ~model (~'? (key-name ~model))))

(defn make-crud-model-edit [model]
  `(~'fetch ~model (~'? (key-name ~model))))

(defn make-crud-model-alter [model]
  `(do 
     (~'? (key-name ~model))
     (-> (~'save ~model ~'input)
         (with-alert :success "Saved"))))

(defn make-crud-model-delete [model]
  `(do
     (~'delete ~model (~'? (key-name ~model)))
     nil))


(defmacro def-crud-actions [model-sym]
  (let [model @(resolve model-sym)
        modelname (table-name model)]
    `(do
       (defaction ~(symbol (str modelname "-list"))
         ~(make-crud-model-list model-sym))
       (defaction ~(symbol (str modelname "-new"))
         ~(make-crud-model-new model-sym))
       (defaction ~(symbol (str modelname "-add"))
         ~(make-crud-model-add model-sym))
       (defaction ~(symbol (str modelname "-show"))
         ~(make-crud-model-show model-sym))
       (defaction ~(symbol (str modelname "-edit"))
         ~(make-crud-model-edit model-sym))
       (defaction ~(symbol (str modelname "-alter"))
         ~(make-crud-model-alter model-sym))
       (defaction ~(symbol (str modelname "-delete"))
         ~(make-crud-model-delete model-sym)))))

(defn with-lookup- [m model system input]
  (let [action-name (str (table-name model) "-list")
        action (resolve-action system {:_action action-name})
        lookup-res (run-action system action {})]
    (merge lookup-res m)))

(defmacro with-lookup [m model]
  `(with-lookup- ~m ~model ~'system ~'input))
