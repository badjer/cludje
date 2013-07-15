(ns cludje.demo.models
  (:use cludje.core
        cludje.types))

(defmodel Cog {:name Str :amt Int :companyid Int})

(defmodel Shift {:date Date :start Time :breaks Timespan :ilikecats Bool})
