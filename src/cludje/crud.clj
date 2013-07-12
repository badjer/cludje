(ns cludje.crud
  (:use cludje.core
        cludje.types))

(def crud-actions [:list :post :get :put :delete])

(defmacro def-crud-actions [model-sym]
  (let [model @(resolve model-sym)
        modelname (table-name model)
        keename (key-name model)]
    `(do
      (defaction ~(symbol (str "list-" modelname "s"))
         (~'query ~model-sym nil))
      (defaction ~(symbol (str "post-" modelname))
         (~'save ~model-sym ~'input))
      (defaction ~(symbol (str "get-" modelname))
         (~'fetch ~model-sym (~'? ~keename)))
      (defaction ~(symbol (str "put-" modelname))
         (~'? ~keename)
         (~'save ~model-sym ~'input))
      (defaction ~(symbol (str "delete-" modelname))
         (~'delete ~model-sym (~'? ~keename))))))

