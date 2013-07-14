(ns cludje.crud
  (:use cludje.core
        cludje.types))

(def crud-actions [:new :show :edit :delete :list])

(defmacro def-crud-actions [model-sym]
  (let [model @(resolve model-sym)
        modelname (table-name model)
        keename (key-name model)]
    `(do
      (defaction ~(symbol (str modelname "-list"))
         {~(keyword (str modelname "s")) (~'query ~model-sym nil)})
      (defaction ~(symbol (str modelname "-new"))
         (~'save ~model-sym ~'input))
      (defaction ~(symbol (str modelname "-show"))
         (~'fetch ~model-sym (~'? ~keename)))
      (defaction ~(symbol (str modelname "-edit"))
         (~'? ~keename)
         (~'save ~model-sym ~'input))
      (defaction ~(symbol (str modelname "-delete"))
         (~'delete ~model-sym (~'? ~keename))))))

