(ns cludje.app
  (:use cludje.core
        cludje.database
        cludje.mailer
        cludje.logger
        cludje.auth
        cludje.dispatcher
        cludje.renderer
        cludje.server))

(defaction default-action {:body "hello world"})

(defn default-system []
  {:db (->MemDb (atom {}))
   :mailer (->MemMailer (atom []))
   :logger (->MemLogger (atom []))
   :auth (->MockAuth (atom false))
   :dispatcher (->Dispatcher (atom {:default default-action}))
   :renderer (->LiteralRenderer)
   :server (->JettyServer 8888 (atom nil) (atom nil))})

(defn make-system 
  "Create a system. Use defaults if none provided"
  ([]
   (make-system {}))
  ([opts]
   (merge (default-system) opts)))

(defn start-system [sys]
  (let [handler (make-ring-handler sys)]
    ; Set the server handler
    (set-handler (:server sys) handler)
    (doseq [subsys (vals sys)]
      (when (extends? IStartable (type subsys))
        (start subsys)))
    sys))

(defn stop-system [sys]
  (doseq [subsys (vals sys)]
    (when (extends? IStartable (type subsys))
      (stop subsys))))


;(defmodel User {:email Email :password Password :name Str}
  ;:require [:email :password])
;
;(defmodel Household 
  ;{:attendee Str :maxguests Int :attending Bool :user_id Int}
  ;:require [:attendee :maxguests :user_id])
;
;(defmodel Guest {:name Str :household_id Int})
;
;(defaction AddHousehold
  ;(let [uid (save User request)]
    ;(save Household (assoc request :user_id uid))))
;
;(defaction AddGuest
  ;(save Guest request))
;
;(defn start-app [])
;;
;(defn stop-app [])



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
