(ns cludje.uiadapter
  (:use cludje.core)
  (:require [cheshire.core :as cheshire]
            [ring.util.response :as response]))


(defn- is-transient-field [field]
  (re-find #"^__" (name field)))

(defn- strip-transient-fields [input]
  "Strip off any fields that start with __"
  (let [victims (filter is-transient-field (keys input))]
    (apply dissoc input victims)))

(defn- is-persistent-field [field]
  (re-find #"^_p_" (name field)))

(defn- ->session [session output]
  (let [victims (filter is-persistent-field (keys output))
        new-vals (select-keys output victims)]
    (merge session new-vals)))

(defn- with-session [input session]
  (merge session input))

(defn- check-output [output]
  (cond
    (map? output) output
    (nil? output) nil
    :else (throw 
            (ex-info "We tried to render something that wasn't a map!  
                     Probably, your action didn't return a map.  
                     Always return a map from actions" {:output output}))))

(defrecord TestUIAdapter [session]
  IUIAdapter
  (parse-input- [self request]
    (with-session request @session))
  (render- [self request output]
    (check-output output)
    (let [new-session (->session @session output)]
      (reset! session new-session) 
      output))
  (is-action- [self request] true))


(defn- assoc-cookies [input request]
  "Read the persistent fields out of the cookie and set them to the input"
  (let [fields (filter is-persistent-field (keys (:cookies request)))]
    (apply merge input
           (for [f fields]
             {(keyword f) (get-in request [:cookies f :value])}))))

(defn- make-cookies [output]
  (let [fields (filter is-persistent-field (keys output))]
    (or 
      (apply merge
             (for [f fields]
               {(name f) {:value (or (get output f nil) "")}}))
      {})))

(defn- cleanup-input [input request]
  (-> input
      (strip-transient-fields)
      (assoc-cookies request)))

(defn is-api-call? [allow-get? request]
  (let [postcheck (or allow-get? (= (:request-method request) :post))
        uri-check (= "/api" (:uri request))]
    (and postcheck uri-check)))

(defn- remove-persistent-fields [output]
  (let [victims (filter is-persistent-field (keys output))]
    (apply dissoc output victims)))



(defn- filter-output [output]
  (cond
    (map? output) (remove-persistent-fields output)
    :else output))

(defrecord WebUIAdapter [allow-api-get?]
  IUIAdapter
  (parse-input- [self request]
    (let [data (get request :params (get request :body))]
      (when (is-api-call? allow-api-get? request)
        (cleanup-input data request))))
  (render- [self request output]
    (check-output output)
    (-> (merge
          {:body (cheshire/generate-string (filter-output output))}
          {:cookies (make-cookies output)})
        (response/content-type "application/json")
        (response/charset "UTF-8")))
  (is-action- [self request] (is-api-call? allow-api-get? request)))

(defn make-WebUIAdapter [opts]
  (->WebUIAdapter (get opts :allow-api-get? false)))
