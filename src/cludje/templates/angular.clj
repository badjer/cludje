(ns cludje.templates.angular
  (:require [clojure.string :as s])
  (:use [hiccup.core :only [html]]
        [hiccup.page :only [html5]]
        cludje.model
        cludje.mold
        cludje.types))

(defn ng-path [& path] 
  (let [res (apply str (map name (filter identity path)))]
    res))

(defn ng-options [& path]
  (str "d.val as d.text for d in " (apply ng-path path)))

(defn select-box [field]
  [:select {:id field :name field :ng-model (ng-path "data." field)
            :ng-options (ng-options "data." field "_options")}
   [:option "Choose"]])


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
  (ng-field [self field] 
    [:input {:id field :name field :type "checkbox" :ng-model (ng-path "data." field)}]))
(extend-type (type Date)
  IAngularField
  (ng-field [self field] (select-box field)))
(extend-type (type Time)
  IAngularField
  (ng-field [self field] (select-box field)))
(extend-type (type Timespan)
  IAngularField
  (ng-field [self field] (select-box field)))
(extend-type (type DateTime)
  IAngularField
  (ng-field [self field] (select-box field)))
                                                   

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

(declare ->js)

(defn map->js [m]
  (str "{" 
       (s/join ", " (for [[k v] m] (str (name k) ": " (->js v))))
       "}"))

(defn ->js [m]
  (cond 
    (nil? m) "null"
    (= java.lang.String (type m)) (str "'" m "'")
    (keyword? m) (str "'" (name m) "'")
    (map? m) (map->js m)
    :else m))


(defn button 
  ([txt k v & in-opts] (button txt (apply hash-map k v in-opts)))
  ([txt passed-opts]
    (let [args (select-keys passed-opts 
                            [:args :allow_return :reload :confirm])
          arg-str (->js args)
          click (when-let [action (:action passed-opts)]
                  {:stop-event "click" :ng-click 
                   (str "action('" (name action) "', " arg-str ")")})
          cancel (when (:cancel passed-opts)
                   {:stop-event "click" :ng-click "cancel()"})
          opts (merge {:type :button :class "btn btn-primary"}
                      cancel
                      click 
                      (dissoc passed-opts :action :cancel :args :confirm 
                              :reload :allow_return))]
      [:button opts txt])))

(defn cancel-button [txt & in-opts]
  (let [passed-opts (apply hash-map in-opts)
        opts (merge {:class "btn" :cancel true}
                    passed-opts)]
    (button txt opts)))

(defn link [txt & args]
  (let [passed-args (apply hash-map args)
        return (when (:return passed-args)
                 {:ng-href (str (:href passed-args) "?_return={{location.pathname}}")})
        opts (merge {:href "#" :class "btn btn-primary"}
                    return
                    (dissoc passed-args :return))]
    [:a opts txt]))


(defn alerts []
  [:alert {:ng-repeat "alert in data.__alerts"
           :type "alert.type" :close "closeAlert($index)"}
   (ng-data "alert.text")])

(defn action-name [model action]
  (str (tablename model) "-" (name action)))

(defn model-form [model title action]
  (let [ac-name (action-name model action)
        fields (fields model)
        invisible (invisible-fields model)
        visible-fields (apply dissoc fields invisible)]
    (form 
      (when title [:h3 {:ng-hide "data._title"} title])
      [:h3 {:ng-show "data._title"} (ng-data "data." :_title)]
      (for [field invisible]
        (ng-field Hidden field))
      (for [[field typ] visible-fields]
        (form-line (ng-field typ field) 
                   field 
                   (friendly-name model field)))
      (when action
        (form-line [:div.button-group 
                    (button "Save" :action ac-name) 
                    (cancel-button "Cancel")])))))

(defn model-title-field [model]
  (let [fts (fields model)]
    (if (:name fts)
      :name
      (first (keys fts)))))

(defn summarize-model [model]
  (let [tablename (tablename model)]
    [:h4 (ng-data tablename "." (model-title-field model))]))

(defn list-item-model [model summarized-markup]
  (let [tablename (tablename model)]
    [:li.span4.thumbnail.well
     {:ng-repeat (str tablename " in data." tablename "s")
      :stop-event "click" :ng-click (str "redirect('/" tablename "/show?_id="
                                         (ng-data tablename "._id") "')")}
     [:div (button "X" :confirm "Are you sure you want to delete?"
                   :action (str tablename "-delete")
                   :args {:_id (ng-data tablename "._id")}
                   :reload true
                   :class "btn btn-danger btn-mini pull-right")
      summarized-markup]]))

(defn list-model [model itemized-markup]
  [:ul.thumbnails itemized-markup])


(defn template-list-body [model listed-markup]
  [:div 
   [:div.pull-right.btn-toolbar
    [:div.btn-group
     (link "New" :href (str "/" (tablename model) "/new") :return true)]]
   [:h3 {:ng-hide "data._title"} "List of " (tablename model)]
   [:h3 {:ng-show "data._title"} (ng-data "data._title")]
   listed-markup])
   ;(model-list model)])

(defn template-edit-body [model]
  (model-form model (str "Edit " (tablename model)) :alter))

(defn template-new-body [model]
  (model-form model (str "New " (tablename model)) :add))

(defn template-show-body [model]
  (let [fields (fields model)
        invisible (invisible-fields model)
        visible-fields (apply dissoc fields invisible)]
    [:div 
     [:h3 {:ng-hide "data._title"} (tablename model)]
     [:h3 {:ng-show "data._title"} (ng-data "data." :_title)]
     (for [field invisible]
       (ng-field Hidden field))
     (for [[field typ] visible-fields] 
       (form-line 
         (ng-data "data." field) 
         field
         (friendly-name field)))]))





; Basic templates
; NOTE: These are NOT the same as the actions you can take on 
; a model - these are purely screens. We're intentionally making
; a separation between UI and data - we're trying not to conflate
; them like rails does

(defn common-layout [body]
  (html (html5 
          [:html {:lang "en" :ng-app "mainapp" :ng-controller "MainCntl" 
                  :ng-cloak "ng-cloak"}
           [:head 
            [:meta {:charset "utf-8"}]
            [:meta {:http-equiv "X-UA-Compatible" :content "IE Edge,chrome 1"}]
            [:meta {:name "viewport" :content "width device-width, initial-scale 1.0"}]
            [:title (ng-data "global.title")]
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
           [:body {:style "padding-top: 0px;"}
            [:div.navbar.navbar-static-top
             [:div.navbar-inner
              [:div.container
               [:a.btn.btn-navbar {:data-toggle "collapse" :data-target ".nav-collapse"}
                [:span.icon-bar]
                [:span.icon-bar]
                [:span.icon-bar]
                ]
               [:span.brand [:a {:href "/"} (ng-data "global.title")]]
               [:div.nav-collapse.collapse
                [:ul.nav
                 [:li {:ng-repeat "item in global.menu"}
                  [:a {:href (ng-data "item.url")} (ng-data "item.text")]]
                 ]
                [:ul.nav.pull-right
                 [:li {:ng-show "global.user.username"} 
                  [:a {:href "#"} "Logged in as " (ng-data "global.user.username")]]
                 [:li {:ng-hide "global.user.username"}
                  [:a {:href (ng-data "global.login_url")} "Login"]]]
                ]
               ]
              ]
             ]
            [:div.container-fluid 
             (alerts)
             body]
            [:footer (ng-data "global.footer")]
            ] 
           [:script {:src "//ajax.googleapis.com/ajax/libs/angularjs/1.1.5/angular.min.js"}]
           ; Only if we need angular-ui-bootstrap - it's mostly just animations etc
           [:script {:src "//cdnjs.cloudflare.com/ajax/libs/angular-ui-bootstrap/0.4.0/ui-bootstrap-tpls.min.js"}]
           [:script {:src "/js/app.js"}]
            ])))

(defn js-app []
  "Serves the angular.js controller and module."
  ;This isn't a static file because I think we'll probably
  ;want to generate this dynamically pretty soon
  "var cludje = angular.module('mainapp', ['ui.bootstrap'], 
      function($routeProvider, $locationProvider){
      });
  cludje.directive('stopEvent', function () {
    return {
        restrict: 'A',
        link: function (scope, element, attr) {
            element.bind(attr.stopEvent, function (e) {
                e.stopPropagation();
            });
        }
    }
  });


  function MainCntl($scope, $http){ 


    var get_querystring = function() { 
      var assoc  = {};
      var decode = function (s) { return decodeURIComponent(s.replace(/\\+/g, ' ')); };
      var queryString = location.search.substring(1); 
      var keyValues = queryString.split('&'); 

      for(var i in keyValues) { 
        var key = keyValues[i].split('=');
        if (key.length > 1) {
          assoc[decode(key[0])] = decode(key[1]);
        }
      } 

      return assoc; 
    };

    var getParameterByName = function(name) {
      name = name.replace(/[\\[]/, \"\\\\\\[\").replace(/[\\]]/, \"\\\\\\]\");
      var regex = new RegExp(\"[\\\\?&]\" + name + \"=([^&#]*)\"),
          results = regex.exec(location.search);
      return results == null ? \"\" : decodeURIComponent(results[1].replace(/\\+/g, \" \"));
    };

    // Figure out what the current action is, based on the url
    var get_default_action = function(){
      // If there's _action in the querystring, use that
      var qsaction = getParameterByName('_action');
      if(qsaction != ''){
        return qsaction;
      }else{
        // If there's no explicit action, call an action that is the
        // same as the name of the template
        var re = /^\\/([^/]+)\\/([^.]+).*$/;
        if(re.test(window.location.pathname)){
          return window.location.pathname.replace(re, \"$1-$2\");
        }
      }
      return null; 
   }; 
    
   var get_default_action_args = function(){
     return get_querystring();
   }; 
      
   var go_back = function(){
     var ret = getParameterByName('_return');
     if(ret != undefined && ret != null && ret != ''){
       window.location = ret;
     }
   };


    var reload = function(opts){
      do_action(get_default_action(), $scope.data, {reload: false, allow_return:false});
    } 

    var bind_data = function(opts, data){
      if(data == null || data == 'null') // Wierd hack for returning null bug
        return;
      if(opts.is_global === true)
        $scope.global = data;
      else
        $scope.data = data;
    };

    var push_alert = function(typ, text){
      if($scope.data.__alerts === undefined || 
          $scope.data.__alerts === null){
        $scope.data.__alerts = [];
      }
      $scope.data.__alerts.push(
        {type: typ, text: text});
    };


    var go_login = function(){
      var login_url = '/global/login';
      login_url += '?_return=' + $scope.location.pathname; 
      window.location = login_url;
    };
      
    var is_successful = function(data){
      return data.__problems === undefined || data.__problemns === null;
    };

    var needs_login = function(response){
      return response.status === 401;
    };

    var is_forbidden = function(response){
      return response.status === 403;
    };

    var is_not_found = function(response){
      return response.status === 404;
    };

    var do_action = function(action, payload, opts){
      payload._action = action;
      var res = $http.post('/api', payload);
      res.then(function(response){
        bind_data(opts, response.data);
        if(is_successful(response.data)){
          if(opts.allow_return != false)
            go_back(opts);
          
          if(opts.reload === true)
            reload(opts);
        }
      },
      function(error_response){
        if(needs_login(error_response)){
          go_login();
        }
        else if(is_forbidden(error_response)){
          push_alert('error', 
            'You are not authorized to perform that action');
        }else if(is_not_found(error_response)){
          push_alert('error',
            'That action could not be found');
        }else{
          push_alert('error',
            'An unknown error has occurred');
        }
      });
    };
      
      
    // Initialize our data
    $scope.data = {};
    $scope.location = {};
    $scope.location.pathname = location.pathname;

    // Seup actions
    
    // A cancel button just goes back to the _return url (if there is one)
    $scope.cancel = function(){
      go_back();
    };

    // We need an action to dismiss alerts
    $scope.closeAlert = function(index){
      $scope.data.__alerts.splice(index,1);
    };


    $scope.action = function(action, opts){
      if(opts === undefined || opts === null){
        opts = {};
      }
      var payload = (opts.args === undefined || opts.args === null) ? 
                      $scope.data : 
                      opts.args;

      var should = true;
      if(opts.confirm != undefined)
        should = confirm(opts.confirm);

      if(should)
        do_action(action, payload, opts);
    };

    $scope.confirm_action = function(message, action, opts){
      var should = confirm(message);
      if(should)
        $scope.action(action, opts);
    };

    $scope.redirect = function(url){
      window.location = url;
    };
      

    // We want to load the global data - this will get us
    // menus, titles, etc
    // We also want to load the page-specific data when we're done,
    // so we'll tell the handler to reload afterwards
    // This code should only get run on page-load
    do_action('global-data', {}, {is_global: true, allow_return: false, 
                                  reload: false});
    do_action(get_default_action(), get_default_action_args(),
        {allow_return: false, reload: false});
  };")


(defn global-login []
  (common-layout 
    (form
      [:h3 {:ng-hide "data._title"} "Login"]
      [:h3 {:ng-show "data._title"} (ng-data "data." :_title)]
      (form-line (ng-field Email :username)
                 :username
                 "Email")
      (form-line (ng-field Password :pwd)
                 :pwd
                 "Password")
      (form-line (button "Login" :action :global-dologin)))))

(defn global-logout []
  (common-layout 
    [:div 
     [:p "You are being logged out..."]
     (link "Click to continue" :href "/global/login" 
           :class "btn btn-primary btn-large")]))
   

(defn template-edit [model]
  (common-layout (template-edit-body model)))
(defn template-new [model]
  (common-layout (template-new-body model)))
(defn template-list [model]
  (common-layout (template-list-body 
                   model (->> (summarize-model model)
                              (list-item-model model)
                              (list-model model)))))
(defn template-show [model]
  (common-layout (template-show-body model)))

(defmacro use-default-templates []
  `(do 
     (defn ~'common-layout [~'body] 
       (cludje.templates.angular/common-layout ~'body))
     (defn ~'js-app []
       (cludje.templates.angular/js-app))
     (defn ~'global-login []
       (cludje.templates.angular/global-login))
     (defn ~'global-logout []
       (cludje.templates.angular/global-logout))
     (defn ~'-template-edit [~'model]
       (cludje.templates.angular/template-edit ~'model))
     (defn ~'-template-new [~'model]
       (cludje.templates.angular/template-new ~'model))
     (defn ~'-template-list [~'model]
       (cludje.templates.angular/template-list ~'model))
     (defn ~'-template-show [~'model]
       (cludje.templates.angular/template-show ~'model))))
