(ns cludje.templatestore
  (:use cludje.core))

(defprotocol ITemplateStore
  "Finds a given template"
  (get-template- [self model action-key]))

(defn get-generic-template-name [modelstore model action-key]
  (when (and model action-key)
    (let [ms (model-name model)]
      (when-let [model (get-model- modelstore ms)]
        (str "-template-" (name action-key))))))

(defrecord GenericTemplateStore [template-ns modelstore]
  ITemplateStore
  (get-template- [self model action-key]
    (let [ms (model-name model)
          model (get-model- modelstore ms)]
      (when model
        (let [templatename (str "-template-" (name action-key)) 
              template (find-in-ns template-ns templatename)]
          (when template
            (template model)))))))

(defn get-template-name [model action-key]
  (when (and model action-key)
    (let [tablename (table-name model)
          action (name action-key)
          tn (str tablename "-" action)]
      (when (not= "-" tn)
        tn))))


(defrecord TemplateStore [template-ns]
  ITemplateStore
  (get-template- [self model action-key]
    (when-let [templatename (get-template-name model action-key)]
      (when-let [template (find-in-ns template-ns templatename)]
        (template)))))

(defrecord CompositeTemplateStore [templatestores]
  ITemplateStore
  (get-template- [self model action-key]
    (first (filter identity (map #(get-template- % model action-key)
                                 templatestores)))))


(defn make-templatestore [system]
  (let [generic (map->GenericTemplateStore system)
        standard (map->TemplateStore system)]
    (->CompositeTemplateStore [standard generic])))

