(ns cludje.app
  (:use cludje.core
        cludje.types))

(defmodel User {:email Email :password Password :name Str}
  :require [:email :password])

(defmodel Household 
  {:attendee Str :maxguests Int :attending Bool :user_id Int}
  :require [:attendee :maxguests :user_id])

(defmodel Guest {:name Str :household_id Int})

(defaction AddHousehold
  (let [uid (save User request)]
    (save Household (assoc request :user_id uid))))

(defaction AddGuest
  (save Guest request))

(defn start-app [])

(defn stop-app [])



;(auth-role AddHousehold :admin)
;(auth-role AddGuest :guest)
;(defn own-house? [request user]
;  (let [household (query Household (select-keys user :user_id))]
;    (= (:household_id request) 
;;       (:household_id household))))

;(auth AddGuest (own? Household))
;
;(auth AddGuest own-house?)
;;(route "/addHousehold" AddHousehold)
;(route "/addGuest" AddGuest)
;
;(auth-role :admin)
