(ns cludje.demo.models
  (:use cludje.model
        cludje.types))

(def Cog (>Model "cog" {:name Str :amt Int :companyid Int} {}))

(def Shift (>Model "shift" {:date Date :start Time :breaks Timespan :ilikecats Bool}
                   {}))
