(ns cludje.app
  (:use cludje.core
        cludje.types))

(defmodel User {:email Email :password Password :name Str}
  :require [:email :password])

(defmodel Household 
  {:attendee Str :maxguests Int :attending Bool :user_id Int}
  :require [:attendee :maxguests :user_id])

(defmodel Guest {:name Str :household_id Int})

;(definteraction AddHousehold [in]
  ;(let [u (save User in)]
    ;(save Household (merge in u))))
;
;(definteraction AddGuest [in]
  ;(save Guest in))




