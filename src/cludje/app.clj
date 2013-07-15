(ns cludje.app
  (:use cludje.core
        cludje.database
        cludje.mailer
        cludje.logger
        cludje.auth
        cludje.login
        cludje.dispatcher
        cludje.renderer
        cludje.server))

(defaction hello-world {:msg "hello world"})

(defn default-system 
  ([] (default-system {}))
  ([{:keys [port model-ns action-ns template-ns default-action] 
     :or {port 8888 default-action hello-world} :as opts}]
   {:db (->MemDb (atom {}))
    :mailer (->MemMailer (atom []))
    :logger (->MemLogger (atom []))
    :login (make-MockLogin false)
    :auth (make-auth mock-auth-fn)
    :dispatcher (make-dispatcher action-ns {:default default-action})
    :renderer (->JsonRenderer)
    :server (jetty port)
    :action-ns action-ns
    :model-ns model-ns
    :template-ns template-ns
    :allow-api-get? false}))

(defn make-system 
  "Create a system. Use defaults if none provided"
  ([]
   (make-system {}))
  ([opts]
   (merge (default-system opts) opts)))


; System stuff

(defn start-system [sys]
  (let [templates (when-let [ts (:template-ns sys)] 
                    (template-handler (:model-ns sys) ts))
        actions (action-handler sys)
        instances (when-let [ts (:template-ns sys)]
                    (template-instance-handler (:model-ns sys) ts))
        handler (ring-handler actions instances templates)]
    ; Set the server handler
    (set-handler- (:server sys) handler)
    (doseq [subsys (vals sys)]
      (when (satisfies? IStartable subsys)
        (start- subsys)))
    sys))


(defn stop-system [sys]
  (doseq [subsys (vals sys)]
    (when (satisfies? IStartable subsys)
      (stop- subsys))))
