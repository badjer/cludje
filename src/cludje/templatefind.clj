(ns cludje.templatefind
  (:use cludje.system
        cludje.util
        cludje.find)
  (:require [clojure.string :as s]))

(defn- operation-name [template-name]
  (first (s/split (name template-name) #"-")))

(defn- model-name [template-name]
  (last (s/split (name template-name) #"-")))

(defn looks-like-generic-template? [sym]
  (let [f @(resolve sym)]
    (and (fn? f) (= 1 (arity f)))))


(defrecord GenericTemplateFinder [generic-template-namespaces]
  ITemplateFinder
  (find-template [self request]
    (let [template-name (? request :template-name) 
          op-name (operation-name template-name) 
          generic-name (str "template-for-" op-name)
          finds (keep identity (map #(find-in-ns % generic-name)
                                    @generic-template-namespaces))
          matches (filter looks-like-generic-template? finds)]
      (when-not (empty? matches)
        (let [moldfinder (? request [:system :mold-finder])
              modelname (model-name template-name)
              moldrequest (assoc-in request [:params :_action] template-name)
              model (find-output-mold moldfinder moldrequest)
              template (first matches)]
          (@(resolve template) model))))))


(defn >GenericTemplateFinder [& generic-template-namespaces]
  (->GenericTemplateFinder (atom generic-template-namespaces)))


(defn looks-like-template? [sym]
  (let [f @(resolve sym)]
    (and (fn? f) (= 0 (arity f)))))

(defrecord SpecificTemplateFinder [template-namespaces]
  ITemplateFinder
  (find-template [self request]
    (let [template-name (? request :template-name)
          finds (keep identity (map #(find-in-ns % template-name) 
                                    @template-namespaces))
          matches (filter looks-like-template? finds)
          template (first matches)]
      (when template
        (@(resolve template))))))

(defn >SpecificTemplateFinder [& template-namespaces]
  (->SpecificTemplateFinder (atom template-namespaces)))


(defrecord CompositeTemplateFinder [sub-finders]
  ITemplateFinder
  (find-template [self request]
    (first (keep #(find-template % request) @sub-finders))))

(defn >CompositeTemplateFinder [generic-template-ns & template-namespaces]
  (let [specifics (map >SpecificTemplateFinder template-namespaces)
        generic (>GenericTemplateFinder generic-template-ns)
        sub-finders (conj (vec specifics) generic)]
    (->CompositeTemplateFinder (atom sub-finders))))
