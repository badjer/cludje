(ns cludje.renderer
  (:use cludje.core)
  (:require [cheshire.core :as cheshire]))

(defrecord LiteralRenderer []
  IRenderer
  (render- [self request output]
    output))

(defrecord JsonRenderer []
  IRenderer
  (render- [self request output]
    (when-not (map? output)
      (throw (ex-info "We tried to render something that wasn't a map!
                      Generally, this means that your action didn't return a map.
                      Always return a map from actions" {})))
    {:body (cheshire/generate-string output)}))

