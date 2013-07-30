(ns cludje.crud
  (:use cludje.core
        cludje.types))

(def crud-actions [:new :add :show :edit :alter :delete :list])

(defmacro def-system-actions []
  `(do
     (defability ~'-system-data-ability
       :* "Global" :anon)
     (defaction ~'global-dologin
       (~'login ~'input))
     (defaction ~'global-login
       {})
     (defaction ~'global-logout
       (~'logout ~'input))
     (defaction ~'global-data
       {:user (~'current-user)
        :menu [{:text "A" :link "/a"} {:text "B" :link "/b"}] 
        :footer "© 2013 Cludje"
        :title "Cludje"})))

(defmacro def-crud-actions [model-sym]
  (let [model @(resolve model-sym)
        modelname (table-name model)
        keename (key-name model)]
    `(do
       (defaction ~(symbol (str modelname "-list"))
         {~(keyword (str modelname "s")) (~'query ~model-sym nil)})
       (defaction ~(symbol (str modelname "-new"))
         {})
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

