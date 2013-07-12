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

(defn angular-layout [body]
  (html (html5 
          [:html {:lang "en"}
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
            [:div.navbar.navbar-inverse.navbar-static-top
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
            body
            [:footer]
            ] 
           [:script {:src "//ajax.googleapis.com/ajax/libs/angularjs/1.0.6/angular.min.js"}]
           [:script {:type "text/javascript"}
            "function MainCntl($scope, $http){ 
              $scope.data = {};
              $scope.action = function(actname){
                // Do some ajax here
                $http.post('/', $scope.data)
                  .success(function(data){
                    $scope.data = data;
                    console.log(data);
                  });
              };
            };"]])))
            


(defn list-template [model]
  (angular-layout
    [:div
     (for [[field typ] (dissoc (field-types model) :_id)]
       (ng typ field))]))
