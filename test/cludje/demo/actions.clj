(ns cludje.demo.actions
  (:use cludje.types
        cludje.core
        cludje.crud
        cludje.demo.models))

(def-system-actions)
(def-crud-actions Cog)
(def-crud-actions Shift)


(defaction shift-new
  {:date_options (date-range (now) -15 15)
   :ilikecats true
   :start_options (time-range (hours 8) (hours 17) (minutes 15))
   :breaks_options (timespan-range (hours 2) (minutes 15))})

