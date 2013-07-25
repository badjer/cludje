(ns cludje.renderer
  (:use cludje.core
        [cludje.parser :only [is-persistent-field]])
  (:require [cheshire.core :as cheshire]
            [ring.util.response :as response]))

(defrecord LiteralRenderer []
  IRenderer
  (render- [self request output]
    output))

(defn- make-cookies [output]
  (let [fields (filter is-persistent-field (keys output))]
    (apply merge
           (for [f fields]
             {(name f) {:value (f output)}}))))

(defn- remove-persistent-fields [output]
  (let [victims (filter is-persistent-field (keys output))]
    (apply dissoc output victims)))

(defn- filter-output [output]
  (cond
    (map? output) (remove-persistent-fields output)
    (nil? output) nil
    :else (throw 
            (ex-info "We tried to render something that wasn't a map!  
                     Probably, your action didn't return a map.  
                     Always return a map from actions" {:output output}))))

(defrecord JsonRenderer []
  IRenderer
  (render- [self request output]
    (-> (merge
          {:body (cheshire/generate-string (filter-output output))}
          {:cookies (make-cookies output)})
        (response/content-type "application/json")
        (response/charset "UTF-8"))))

