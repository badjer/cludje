(ns cludje.templates.angular
  (:use [hiccup.core :only [html]]
        [hiccup.page :only [html5]]
        cludje.core
        cludje.types))

(defprotocol IAngularField
  (ng [self field] "Render some angular.js markup with the id field"))

(extend-type (type Str)
  IAngularField
  (ng [self field] [:input {:id field :type "text" :name field}]))
(extend-type (type Password)
  IAngularField
  (ng [self field] [:input {:id field :type "password" :name field}]))
(extend-type (type Int)
  IAngularField
  (ng [self field] [:input {:id field :type "text" :name field}]))
;(extend-protocol IAngularField
  ;(type Str)
  ;(ng [self field v] [:input {:id field :type "text" :name field} (show v)])
  ;(type Int)
  ;(ng [self field v] [:input {:id field :type "text" :name field} (show v)])
  ;(type Password)
  ;(ng [self field v] [:input {:id field :type "password" :name field} (show v)]))

(defn angular-layout [title menu-opts body]
  (let [output (html (html5 [:html [:head [:title title]] 
                             [:body menu-opts body ]]))]
    output))

(defn list-template [model]
  (angular-layout "Test" {}
    [:div
     (for [[field typ] (dissoc (field-types model) :_id)]
       (ng typ field))]))
