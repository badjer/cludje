(ns cludje.templatestore-test
  (:use midje.sweet
        cludje.modelstore
        cludje.core
        cludje.types
        cludje.templatestore))

(defmodel Cog {:amt Int})

(defn -template-edit [model] "Hello")

(let [ms (->ModelStore 'cludje.templatestore-test)
      gts (->GenericTemplateStore 'cludje.templatestore-test ms)]
  (facts "GenericTemplateStore"
    (get-template- gts Cog :edit) => "Hello"
    (get-template- gts Cog "edit") => "Hello"
    (get-template- gts "Cog" :edit) => "Hello"
    (get-template- gts "Cog" "edit") => "Hello"
    (get-template- gts "cog" :edit) => "Hello"
    (get-template- gts "cog" "edit") => "Hello"
    (get-template- gts nil :edit) => nil
    (get-template- gts nil nil) => nil
    (get-template- gts "" "") => nil
    (get-template- gts "Cog" :foo) => nil
    (get-template- gts Cog :foo) => nil))

(defn cog-show [] "Show")
(defn foo-show [] "Foo")

(let [ts (->TemplateStore 'cludje.templatestore-test)]
  (facts "TemplateStore"
    (get-template- ts Cog :show) => "Show"
    (get-template- ts Cog "show") => "Show"
    (get-template- ts "Cog" :show) => "Show"
    (get-template- ts "Cog" "show") => "Show"
    (get-template- ts nil :show) => nil
    (get-template- ts nil nil) => nil
    (get-template- ts "" "") => nil
    (get-template- ts "Cog" :foo) => nil
    (get-template- ts Cog :foo) => nil)
  (facts "TemplateStore should still serve, even if the model isn't found,
         so long as the fn exists"
    (get-template- ts "Foo" :show) => "Foo"
    (get-template- ts "Foo" "show") => "Foo"
    (get-template- ts "foo" :show) => "Foo"
    (get-template- ts "foo" "show") => "Foo"))

