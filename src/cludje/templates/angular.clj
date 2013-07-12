(ns cludje.templates.angular
  (:require [clojure.string :as s])
  (:use [hiccup.core :only [html]]
        [hiccup.page :only [html5]]
        cludje.core
        cludje.types))

(defn ng-path [& path] (apply str (map name path)))

(defprotocol IAngularField
  (ng-field [self field] "Render some angular.js markup with the id field"))

(def Hidden
  (reify IAngularField
    (ng-field [self field] [:input {:id field :type "hidden" :name field :ng-model (ng-path "data." field)}])))


(extend-type (type Str)
  IAngularField
  (ng-field [self field] [:input {:id field :type "text" :name field :ng-model (ng-path "data." field)}]))
(extend-type (type Email)
  IAngularField
  (ng-field [self field] [:input {:id field :type "text" :name field :ng-model (ng-path "data." field)}]))
(extend-type (type Password)
  IAngularField
  (ng-field [self field] [:input {:id field :type "password" :name field :ng-model (ng-path "data." field)}]))
(extend-type (type Int)
  IAngularField
  (ng-field [self field] [:input {:id field :type "text" :name field :ng-model (ng-path "data." field)}]))
(extend-type (type Money)
  IAngularField
  (ng-field [self field] [:input {:id field :type "text" :name field :ng-model (ng-path "data." field)}]))
(extend-type (type Bool)
  IAngularField
  (ng-field [self field] [:input {:id field :type "text" :name field :ng-model (ng-path "data." field)}]))
(extend-type (type Date)
  IAngularField
  (ng-field [self field] [:input {:id field :type "text" :name field :ng-model (ng-path "data." field)}]))
(extend-type (type Time)
  IAngularField
  (ng-field [self field] [:input {:id field :type "text" :name field :ng-model (ng-path "data." field)}]))
(extend-type (type Timespan)
  IAngularField
  (ng-field [self field] [:input {:id field :type "text" :name field :ng-model (ng-path "data." field)}]))
(extend-type (type DateTime)
  IAngularField
  (ng-field [self field] [:input {:id field :type "text" :name field :ng-model (ng-path "data." field)}]))
                                                   

(defn ng-data [& path] (str "{{" (apply str path) "}}"))

(defn angular-layout [body]
  (html (html5 
          [:html {:lang "en" :ng-app "mainapp"}
           [:head 
            [:meta {:charset "utf-8"}]
            [:meta {:http-equiv "X-UA-Compatible" :content "IE Edge,chrome 1"}]
            [:meta {:name "viewport" :content "width device-width, initial-scale 1.0"}]
            [:title "Cludje"]
            [:meta {:http-equiv "Content-Type" :content "text/html; charset utf-8"}]

            "<!--[if lt IE 9] 
            <script src='//cdnjs.cloudflare.com/ajax/libs/html5shiv/3.6.1/html5shiv.js' type 'text/javascript'></script>
            <![endif]-->"
            [:link {:href "//netdna.bootstrapcdn.com/twitter-bootstrap/2.3.2/css/bootstrap-combined.min.css" :rel "stylesheet"}]

            [:link {:href "/img/apple-touch-icon-144x144-precomposed.png" :rel "apple-touch-icon-precompiled"}] 
            [:link {:href "/img/apple-touch-icon-114x114-precomposed.png" :rel "apple-touch-icon-precompiled"}]
            [:link {:href "/img/apple-touch-icon-72x72-precomposed.png" :rel "apple-touch-icon-precompiled"}]
            [:link {:href "/img/apple-touch-icon--precomposed.png" :rel "apple-touch-icon-precompiled"}]

            [:link {:href "/img/favicon.ico" :rel "shortcut icon"}]
            ]
           [:body {:style "padding-top: 0px;" :ng-controller "MainCntl"}
            [:div.navbar.navbar-static-top
             [:div.navbar-inner
              [:div.container
               [:a.btn.btn-navbar {:data-toggle "collapse" :data-target ".nav-collapse"}
                [:span.icon-bar]
                [:span.icon-bar]
                [:span.icon-bar]
                ]
               [:a.brand {:href "/"} [:strong "Cludje"]]
               [:div.nav-collapse.collapse
                [:ul.nav
                 [:li [:a {:href "/a"} "A"]]
                 [:li [:a {:href "/b"} "B"]]
                 ]
                [:ul.nav.pull-right]
                ]
               ]
              ]
             ]
            [:div.container-fluid body]
            [:footer]
            ] 
           [:script {:src "//ajax.googleapis.com/ajax/libs/angularjs/1.1.5/angular.min.js"}]
           [:script {:type "text/javascript"}
            "angular.module('mainapp', [], 
                function($routeProvider, $locationProvider){
              });

            function MainCntl($scope, $http){ 
              $scope.data = {};
              $scope.action = function(actname){
                // Do some ajax here
                $scope.data.action = actname;
                console.log('Calling ' + actname);
                console.log('With: ' + $scope.data);
                $http.post('/', $scope.data)
                  .success(function(data){
                    $scope.data = data;
                    console.log(data);
                  });
              };
            };"]
            ])))


(defn problem [field]
  [:p.help-inline {:ng-show (str "data.__problems." field)}
   (ng-data "data.__problems." field)])

(defn problem-list []
  [:p.text-error {:ng-repeat "problem in data.__problems"}
   (ng-data "problem") ": " (ng-data "problem.msg")])

(defn form-to [opts & contents]
  (let [attrs (merge {:method "POST" :class "form-horizontal"} opts)]
    [:form attrs contents]))

(defn form [& contents]
  (apply form-to {} contents))


(defn form-line
  ([control]
   (form-line control nil))
  ([control field]
    [:div.control-group
     (when field
       [:label.control-label {:for field} (friendly-name field)])
     [:div.controls control]]))

(defn button [txt & args]
  (let [passed-opts (apply hash-map args)
        click (when-let [a (:action passed-opts)]
                {:ng-click (str "action('" (name a) "')")})
        opts (merge {:type :button :class "btn btn-primary"
                     :value txt} 
                    click
                    (dissoc passed-opts :action))]
    [:input opts]))

(defn action-name [model action]
  (str (name action) "-" (table-name model)))

(defn _form-template [model title action]
  (let [ac-name (action-name model action)]
    (form 
      (when title [:h3 title])
      (ng-field Hidden :_id)
      (for [[field typ] (dissoc (field-types model) :_id)] 
        (form-line (ng-field typ field) field))
      (when action
        (form-line (button "Save" :action ac-name))))))



; Basic templates
; NOTE: These are NOT the same as the actions you can take on 
; a model - these are purely screens. We're intentionally making
; a separation between UI and data - we're trying not to conflate
; them like rails does

(defn edit-template [model]
  (angular-layout
    (_form-template model (str "Edit " (table-name model)) :update)))

(defn new-template [model]
  (angular-layout
    (_form-template model (str "New " (table-name model)) :add)))

(defn index-template [model]
  (angular-layout
    [:h3 "List of " (table-name model)]))

(defn show-template [model]
  (angular-layout
    [:h3 "Printout of one " (table-name model)]))
