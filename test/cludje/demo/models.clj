(ns cludje.demo.models
  (:use cludje.model
        cludje.mold
        cludje.types))

(def Cog (>Model {:name Str :amt Int :companyid Int} {:modelname "cog"}))

(def Shift (>Model {:date Date :start Time :breaks Timespan :ilikecats Bool}
                   {:modelname "shift"}))

(def date-test-output (>Mold {:stuff Anything :echo Date :date Date} {}))
