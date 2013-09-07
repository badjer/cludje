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

(defn http-401 [] {:status 401})
(defn http-403 [] {:status 403})

(defn handle-web-exception [ex]
  (let [exd (ex-data ex)]
    (cond
      (:__notfound exd) nil ; Return a 404
      (:__notloggedin exd) (http-401)
      (:__unauthorized exd) (http-403)
      :else ex)))

(defn wrap-web-exception-handling [f]
  (fn [request]
    (try
      (f request)
      (catch clojure.lang.ExceptionInfo ex
        (let [handled (handle-web-exception ex)]
          (if (= ex handled)
            (throw ex)
            handled))))))
