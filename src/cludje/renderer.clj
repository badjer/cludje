(ns cludje.renderer
  (:use cludje.core)
  (:require [cheshire.core :as cheshire]
            [ring.util.response :as response]))

(defrecord LiteralRenderer []
  IRenderer
  (render- [self request output]
    output))

(defrecord JsonRenderer []
  IRenderer
  (render- [self request output]
    (when-not (or (map? output) (nil? output))
      (throw (ex-info "We tried to render something that wasn't a map!
                      Generally, this means that your action didn't return a map.
                      Always return a map from actions" {:output output})))
    (-> {:body (cheshire/generate-string output)}
        (response/content-type "application/json")
        (response/charset "UTF-8"))))

