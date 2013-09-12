(ns cludje.demo.actions
  (:use cludje.types
        cludje.crud
        cludje.util
        cludje.authorize
        cludje.demo.models))

;(def-system-actions)
(def-crud-actions Cog)
(def-crud-actions Shift)

(def ab-cog (>Ability
  :delete Cog true
  :* Shift true
  :* Cog true))


(defn new-shift [request]
  {:_title "NEW STUFF GOES HERE!"
   :date_options (date-range (now) -15 15)
   :ilikecats true
   :start_options (time-range (hours 8) (hours 17) (minutes 15))
   :breaks_options (timespan-range (hours 2) (minutes 15))})

(defn date-test [request]
  (let [dt (parse Date (?? request [:input :a]))]
    (println dt)
    {:stuff {:x 1} :echo dt :date "2013-09-10T07:00:00.000Z"}))
