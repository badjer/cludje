(ns cludje.web
  (:require [cheshire.core :as cheshire]
            [ring.middleware.json :as json]
            [ring.middleware.resource :as resource]
            [ring.middleware.file-info :as file-info]
            [ring.middleware.params :as params]
            [ring.middleware.cookies :as cookies]
            [ring.middleware.keyword-params :as kw]
            [ring.util.response :as response]))

(def ring-parser
  (-> identity
      ;(file-info/wrap-file-info)
      (cookies/wrap-cookies)
      (kw/wrap-keyword-params)
      (json/wrap-json-params)
      (params/wrap-params)))

(defn- check-output [output]
  (cond
    (map? output) output
    (nil? output) nil
    :else (throw 
            (ex-info "We tried to render something that wasn't a map!  
                     Probably, your action didn't return a map.  
                     Always return a map from actions" {:output output}))))

(defn json-respond [output]
  (check-output output)
  (-> {:body (cheshire/generate-string output)}
      (response/content-type "application/json")
      (response/charset "UTF-8")))


