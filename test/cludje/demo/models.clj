(ns cludje.demo.models
  (:use cludje.model
        cludje.types))

(def Cog (>Model {:name Str :amt Int :companyid Int} {:modelname "cog"}))

(def Shift (>Model {:date Date :start Time :breaks Timespan :ilikecats Bool}
                   {:modelname "shift"}))
