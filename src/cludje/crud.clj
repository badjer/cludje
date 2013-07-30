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


(defmacro def-crud-actions [model-sym]
  (let [model @(resolve model-sym)
        modelname (table-name model)
        keename (key-name model)
        defs (defaults model)]
    `(do
       (defaction ~(symbol (str modelname "-list"))
         {~(keyword (str modelname "s")) (~'query ~model-sym nil)})
       (defaction ~(symbol (str modelname "-new"))
         ~defs)
       (defaction ~(symbol (str modelname "-add"))
         (-> (~'insert ~model-sym ~'input)
             (with-alert :success "Saved")))
       (defaction ~(symbol (str modelname "-show"))
         (~'fetch ~model-sym (~'? ~keename)))
       (defaction ~(symbol (str modelname "-edit"))
         (-> (~'fetch ~model-sym (~'? ~keename))
             (with-alert :success "Saved")))
       (defaction ~(symbol (str modelname "-alter"))
         (~'? ~keename)
         (~'save ~model-sym ~'input))
       (defaction ~(symbol (str modelname "-delete"))
         (~'delete ~model-sym (~'? ~keename))
         nil))))

(defn with-lookup- [m system model]
  (let [action-name (str (table-name model) "-list")
        action (resolve-action system {:_action action-name})
        action-paras {:isarchived false}
        lookup-res (run-action system action action-paras)]
    (merge lookup-res m)))

(defmacro with-lookup [m model]
  `(with-lookup- ~m ~'system ~model))

