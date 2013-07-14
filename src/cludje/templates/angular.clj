(ns cludje.templates.angular
  (:require [clojure.string :as s])
  (:use [hiccup.core :only [html]]
        [hiccup.page :only [html5]]
        cludje.core
        cludje.types))

(defn ng-path [& path] 
  (let [res (apply str (map name (filter identity path)))]
    res))

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
                                                   

(defn ng-data [& path] (str "{{" (apply ng-path path) "}}"))

(defn problem [field]
  [:p.help-inline {:ng-show (ng-path "data.__problems." field)}
   (ng-data "data.__problems." field)])

(defn form-to [opts & contents]
  (let [attrs (merge {:method "POST" :class "form-horizontal"} opts)]
    [:form attrs contents]))

(defn form [& contents]
  (apply form-to {} contents))


(defn form-line
  ([control]
   (form-line control nil nil))
  ([control field label]
    [:div.control-group 
     {:ng-class (str "{error: " 
                     (ng-path "data.__problems." field) " != undefined}")}
     (when label
       [:label.control-label {:for field} label])
     [:div.controls 
      control
      (when field (problem field))
      ]]))

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
  (str (table-name model) "-" (name action)))

(defn _form-template [model title action]
  (let [ac-name (action-name model action)
        fields (field-types model)
        invisible (? (meta model) :invisible)
        visible-fields (apply dissoc fields invisible)]
    (form 
      (when title [:h3 title])
      (for [field invisible]
        (ng-field Hidden field))
      (for [[field typ] visible-fields]
        (form-line (ng-field typ field) 
                   field 
                   (friendly-name model field)))
      (when action
        (form-line (button "Save" :action ac-name))))))

(defn model-title-field [model]
  (let [fts (field-types model)]
    (if (:name fts)
      :name
      (first (keys fts)))))

(defn _summarize-template [model]
  (let [tablename (table-name model)]
    [:div
     [:h4 (ng-data tablename "." (model-title-field model))]]))

(defn _item-template [summarize-template model] 
  [:li.span4.thumbnail.well
   (summarize-template model)])

(defn _list-template [item-template model]
  (let [tablename (table-name model)]
    [:ul.thumbnails {:ng-repeat (str tablename " in data." tablename "s")}
     (item-template model)]))




; Basic templates
; NOTE: These are NOT the same as the actions you can take on 
; a model - these are purely screens. We're intentionally making
; a separation between UI and data - we're trying not to conflate
; them like rails does

(defn common-layout [body]
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
            [:div.container-fluid 
             body]
            [:footer]
            ] 
           [:script {:src "//ajax.googleapis.com/ajax/libs/angularjs/1.1.5/angular.min.js"}]
           [:script {:src "/templates/js/app.js"}]
            ])))

(defn js-app []
  "Serves the angular.js controller and module."
  ;This isn't a static file because I think we'll probably
  ;want to generate this dynamically pretty soon
  "angular.module('mainapp', [], 
      function($routeProvider, $locationProvider){
    });

  function MainCntl($scope, $http){ 
    // Initialize our data
    $scope.data = {};

    // Define our action - an action for calling actions... named action
    // That might be confusing
    $scope.action = function(actname){
      // Set the server-side action we want to call
      $scope.data.action = actname;
      console.log('Calling ' + actname);
      $http.post('/api', $scope.data)
        .success(function(data){
          // Set the result of the server action to our scope
          $scope.data = data;
          console.log(data);
        });
    };

    // Figure out what the current action is, based on the url
    var actname = function(){
      if(window.location.hash){
        return window.location.hash.substring(1);
      //}else{
        // If there's no hash, call an action that is the
        // same as the name of the template
        //var re = /^\\/templates\\/([^\\/]+)\\/([^\\/.]+)\\..*$/;
        //if(re.test(window.location.pathname)){
          //return window.location.pathname.replace(re, \"$1-$2\");
        //}
      }
      return null;
    };

    // Ok, the only other thing we want to do is initialize
    // with our first-time data. To do this, we'll pull the 
    // action name and params off the url hash
    // This code should only get run once (when the page is first
    // loaded)
    if(actname()){
      $scope.action(actname());
    }
  };")

(defn template-edit [model]
  (common-layout
    (_form-template model (str "Edit " (table-name model)) :edit)))

(defn template-new [model]
  (common-layout
    (_form-template model (str "New " (table-name model)) :new)))

(defn template-index [model]
  (common-layout
    [:div 
     [:h3 "List of " (table-name model)]
     (_list-template 
       (partial _item-template _summarize-template)
       model)]))

(defn template-show [model]
  (let [fields (field-types model)
        invisible (? (meta model) :invisible)
        visible-fields (apply dissoc fields invisible)]
    (common-layout
      [:div 
       [:h3 "Printout of one " (table-name model)]
       (for [field invisible]
         (ng-field Hidden field))
       (for [[field typ] visible-fields] 
         (form-line 
           (ng-data "data." field) 
           field
           (friendly-name field)))])))

(defmacro use-default-templates []
  `(do 
     (defn ~'common-layout [~'body] 
       (cludje.templates.angular/common-layout ~'body))
     (defn ~'js-app []
       (cludje.templates.angular/js-app))
     (defn ~'template-edit [~'model]
       (cludje.templates.angular/template-edit ~'model))
     (defn ~'template-new [~'model]
       (cludje.templates.angular/template-new ~'model))
     (defn ~'template-list [~'model]
       (cludje.templates.angular/template-index ~'model))
     (defn ~'template-show [~'model]
       (cludje.templates.angular/template-show ~'model))))
