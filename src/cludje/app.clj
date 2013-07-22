(ns cludje.app
  (:use cludje.core
        cludje.database
        cludje.mailer
        cludje.logger
        cludje.auth
        cludje.login
        cludje.modelstore
        cludje.actionparser
        cludje.actionstore
        cludje.renderer
        cludje.parser
        cludje.templatestore
        cludje.server))

(defaction hello-world {:msg "hello world"})

(defn default-system 
  ([] (default-system {}))
  ([config]
   (let [defaults {:port 8888 :allow-api-get? false 
                   :default-action hello-world}
         opts (merge defaults config)]
     {:db (->MemDb (atom {}))
      :mailer (->MemMailer (atom []))
      :logger (->MemLogger (atom []))
      :login (make-MockLogin false)
      :auth (if-let [an (:action-ns opts)] 
              (make-auth-from-ns opts) 
              (make-auth mock-auth-fn))
      :actionparser (map->ActionParser opts)
      :actionstore (map->ActionStore opts)
      :modelstore (map->ModelStore opts)
      :parser (make-webinputparser opts)
      :renderer (map->JsonRenderer opts)
      :server (jetty opts)})))

(defn make-system 
  "Create a system. Use defaults if none provided"
  ([]
   (make-system {}))
  ([opts]
   (merge (default-system opts) opts)))


; System stuff

(defn start-system [sys]
  ; TODO: Clean this up - this is ugly - we have to assoc a 
  ; new thing onto the system before we can build the handlers
  (let [templatestore (make-templatestore sys)
        with-tpls (assoc sys :templatestore templatestore)
        templates (template-handler with-tpls)
        actions (action-handler with-tpls)
        handler (ring-handler actions templates)]
    ; Set the server handler
    ; TODO: Just move this all to the system-building fn
    (set-handler- (:server sys) handler)
    (doseq [subsys (vals sys)]
      (when (satisfies? IStartable subsys)
        (start- subsys)))
    sys))


(defn stop-system [sys]
  (doseq [subsys (vals sys)]
    (when (satisfies? IStartable subsys)
      (stop- subsys))))
