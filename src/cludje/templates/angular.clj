(ns cludje.templates.angular
  (:use [hiccup.core :only [html]]
        [hiccup.page :only [html5]]
        cludje.core
        cludje.types))

(defprotocol IAngularField
  (ng [self field v] "Render v as some angular.js markup with the id field"))

(extend-type (type Str)
  IAngularField
  (ng [self field v] [:input {:id field :type "text" :name field :value (show Str v)}]))
(extend-type (type Password)
  IAngularField
  (ng [self field v] [:input {:id field :type "password" :name field :value (show Password v)}]))

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

(defn list-model [model m]
  [:div
   (for [[field typ] (field-types model)]
     (ng typ field (get m field)))])
