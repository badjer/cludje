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
    (cheshire/generate-string output)))

