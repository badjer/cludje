(ns cludje.templatestore
  (:use cludje.core))

(defprotocol ITemplateStore
  "Finds a given template"
  (get-template- [self model action-key]))

(defrecord GenericTemplateStore [template-ns modelstore]
  ITemplateStore
  (get-template- [self model action-key]
    (let [ms (model-name model)
          model (get-model- modelstore ms)
          templatename (str "-template-" (name action-key))
          template (find-in-ns template-ns templatename)]
      (when (and template model)
        (template model)))))

(defrecord TemplateStore [template-ns]
  ITemplateStore
  (get-template- [self model action-key]
    (let [templatename (str (table-name model) "-" (name action-key))
          template (find-in-ns template-ns templatename)]
      (when template
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

