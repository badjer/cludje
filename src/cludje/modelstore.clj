(ns cludje.modelstore
  (:use cludje.core))

(defrecord ModelStore [model-ns]
  IModelStore
  (get-model- [self modelname]
    (when-let [model (find-in-ns model-ns modelname :cludje-model)]
      model)))

