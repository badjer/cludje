(ns cludje.crud
  (:use cludje.action 
        cludje.util
        cludje.system
        cludje.model
        cludje.mold
        cludje.types))

(def crud-actions [:new :add :show :edit :alter :delete :list])

; (defn global-dologin [request]
;   (with-action-dsl request
;     (log-in request)))
; 
; (defn global-login [request]
;   {})
; 
; (defn global-logout [request]
;   (with-action-dsl request
;     (log-out request)))
; 
; (defn global-data [request]
;   (with-action-dsl request
;     {:user (current-user)
;      :menu [{:text "A" :url "/a"} {:text "B" :url "/b"}]
;      :footer "© 2013 Cludje"
;      :title "Cludje"}))
 
(defn realize-one [request v]
  "Try to get a final-value for v.
  If it's a var, deref it. If it's a fn, call it with [system input]"
  (cond
    (fn? v) (v request)
    (var? v) (realize-one request @v)
    :else v))

(defn realize-map [request m]
  (into {} (for [[k v] m] [k (realize-one request v)])))

(defn default-list-params [model request]
  (let [parts (partitions model)
        defaults (field-defaults model)]
    ; This is a bit complicated because of optimization
    ; We want parts-def to be (select-keys (realize-map defs)),
    ; but we do an extra internal select-keys inside of 
    ; realize-map in order to avoid evaling any default
    ; fields that we don't need
    ; The reason we add a (make model..) onto this is because
    ; we want to handle the case where the default is of the 
    ; wrong type - this will convert it
    (select-keys 
      (parse model 
             (realize-map request 
                          (select-keys defaults parts))) 
      parts)))

(defn list-params [model request]
  (let [default-params (default-list-params model request)
        input (?? request :input)
        parsed (parse model input)
        supplied-parts (clojure.set/intersection
                         (set (partitions model))
                         (set (keys input)))
        input-parts (select-keys parsed supplied-parts)]
    (merge default-params input-parts)))


(defn model-defaults [model request]
  (let [input (?? request :input)
        defaults (field-defaults model)
        realized-defaults (realize-map request defaults)
        parsed-defaults (parse model realized-defaults)]
    (select-keys parsed-defaults (keys defaults))))

(defn build [model request]
  "Applies all the defaults when make-ing the model"
  (let [input (?! request :input)
        defs (model-defaults model request)
        paras (merge defs input)]
    (make model paras)))


(defn crud-model-list [model request]
  (let [input (?? request :input)
        listkee (keyword (str (tablename model) "s"))
        query-paras (list-params model request)]
    (with-action-dsl request
      {listkee (query model query-paras)})))

(defn crud-model-new [model request]
  (let [defaults (field-defaults model)]
    (realize-map request defaults)))


(defn crud-model-add [model request]
  (with-action-dsl request
    (let [built (build model request)]
      (-> (insert model built)
          (with-alert :success "Saved")))))

(defn crud-model-show [model request]
  (let [keyfield (keyname model)]
    (with-action-dsl request
      (fetch model (?in keyfield)))))

(defn crud-model-edit [model request]
  (with-action-dsl request
    (fetch model (?in (keyname model)))))

(defn crud-model-alter [model request]
  (with-action-dsl request 
    ; Ensure we've got the key
    (?in (keyname model))
    (-> (save model input)
        (with-alert :success "Saved"))))

(defn crud-model-delete [model request]
  (let [keyfield (keyname model)]
    (with-action-dsl request
      (delete model (?in keyfield))
      nil)))


(defmacro def-crud-actions [model-sym]
  (let [model @(resolve model-sym)
        modelname (tablename model)]
    `(do
       (defn ~(symbol (str "list-" modelname)) [request#]
         (crud-model-list ~model-sym request#))
       (defn ~(symbol (str "new-" modelname)) [request#]
         (crud-model-new ~model-sym request#))
       (defn ~(symbol (str "add-" modelname)) [request#]
         (crud-model-add ~model-sym request#))
       (defn ~(symbol (str "show-" modelname)) [request#]
         (crud-model-show ~model-sym request#))
       (defn ~(symbol (str "edit-" modelname)) [request#]
         (crud-model-edit ~model-sym request#))
       (defn ~(symbol (str "alter-" modelname)) [request#]
         (crud-model-alter ~model-sym request#))
       (defn ~(symbol (str "delete-" modelname)) [request#]
         (crud-model-delete ~model-sym request#))
       )))

(defn with-lookup [request m model]
  (let [action-name (str "list-" (tablename model))
        actionfinder (? request [:system :action-finder])
        af-request (assoc request :params {:_action action-name})
        action (find-action actionfinder af-request)
        lookup-res (action request)]
    (merge lookup-res m)))

(defmacro with-crud-dsl [request & forms]
  `(let [~'with-lookup (partial with-lookup ~request)
         ~'build #(build % ~request)]
     ~@forms))
