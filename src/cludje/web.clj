(ns cludje.web
  (:use cludje.util
        cludje.system)
  (:require [cheshire.core :as cheshire]
            [ring.middleware.session :as sess]
            [ring.middleware.json :as json]
            [ring.middleware.resource :as resource]
            [ring.middleware.file-info :as file-info]
            [ring.middleware.params :as params]
            [ring.middleware.cookies :as cookies]
            [ring.middleware.keyword-params :as kw]
            [ring.util.response :as response]))

(defn wrap-ring-middleware 
  ([f] (wrap-ring-middleware f {}))
  ([f opts]
    (-> f
        (cookies/wrap-cookies)
        (sess/wrap-session (get opts :session {}))
        (kw/wrap-keyword-params)
        (json/wrap-json-params)
        (params/wrap-params))))

(defn assert-json-renderable [result]
  (cond
    (map? result) result
    (nil? result) nil
    :else (throw 
            (ex-info "We tried to render something that wasn't a map!  
                     Probably, your action didn't return a map.  
                     Always return a map from actions" {:result result}))))

(defn http-401 [] {:status 401})
(defn http-403 [] {:status 403})

(defn try-lower-web-exception [ex]
  (let [exd (ex-data ex)]
    (cond
      (:__notfound exd) nil ; Return a 404
      (:__notloggedin exd) (http-401)
      (:__unauthorized exd) (http-403)
      :else ex)))

(defn throw-exception [request ex]
  (log (? request [:system :logger]) (str "Error!\n\n" ex "\n\n" (ex-data ex)))
  (throw ex))

(defn wrap-web-exception-handling [f]
  (fn [request]
    (try
      (f request)
      (catch clojure.lang.ExceptionInfo ex
        (let [handled (try-lower-web-exception ex)]
          (if (= ex handled)
            (throw-exception request ex)
            handled))))))
