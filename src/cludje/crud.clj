(ns cludje.crud
  (:use cludje.core
        cludje.types))

(def crud-actions [:new :add :show :edit :alter :delete :list])

(defn with-alert [m text typ]
  (update-in m [:__alerts] conj {:text text :type typ}))

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
             (with-alert "Saved" :success)))
       (defaction ~(symbol (str modelname "-show"))
         (~'fetch ~model-sym (~'? ~keename)))
       (defaction ~(symbol (str modelname "-edit"))
         (-> (~'fetch ~model-sym (~'? ~keename))
             (with-alert "Saved" :success)))
       (defaction ~(symbol (str modelname "-alter"))
         (~'? ~keename)
         (~'save ~model-sym ~'input))
       (defaction ~(symbol (str modelname "-delete"))
         (~'delete ~model-sym (~'? ~keename))))))

