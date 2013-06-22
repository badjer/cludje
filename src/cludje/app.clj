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
   :auth (make-MockAuth (atom false))
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
;
;(auth :update company (= user.companyid company.companyid))
;(auth :delete TimeEntry (= user.companyid (?in :timeentry :companyid)))
;(auth :delete TimeEntry (same? :companyid user company))
;(auth :delete TimeEntry (owner? user company))
;(can :delete TimeEntry (owner? user (:companyid timeentry)))
;(can :all Project (owner? user project))
;(can :delete TimeEntry (owner? user timeentry))
;(can? :delete timeentry)
;
;(can :add TimeEntry (owns-company? timeentry))
;(can :add TimeEntry owns-company?)
;(can :delete TimeEntry owns-company?)
;(can :all TimeEntry owns-company?)
;(can :add TimeEntry works-company?)
;
;
;
;(defaction add-timeentry
;  (let [timeentry (make TimeEntry input)]
;    (can! :add timeentry)
;    (save timeentry)
;
;(defcontroller foosusm
;  (defthat timeentry (or (fetch TimeEntry (:id input)) TimeEntry))
;  (auth-on timeentry)
;  (defaction add
;    (auth! :add timeentry)))
;
;
;(defcontroller TimeEntry
;  (defaction add
;    (auth! :add timeentry)
;    (save timeentry))
;  (defaction delete
;    (auth! :delete timeentry)
;    (remove timeentry)))
;
;(defaction add-timeentry
;  (let [timeentry (make TimeEntry input)]
;    (auth! :add timeentry)))
;
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
