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
; 
(defn realize-one [request v]
  "Try to get a final-value for v.
  If it's a var, deref it. If it's a fn, call it with [system input]"
  (cond
    (fn? v) (v request)
    (var? v) (realize-one request @v)
    :else v))

(defn realize-map [request m]
  (into {} (for [[k v] m] [k (realize-one request v)])))

(defn get-model [request]
  (let [model-sym (? request :input-mold-sym)]
    @(resolve model-sym)))


(defn crud-model-list [request]
  (let [model (get-model request)
        input (?? request :input)
        listkee (keyword (str (tablename model) "s"))
        parts (partitions model)
        defs (field-defaults model)
        ; This is a bit complicated because of optimization
        ; We want parts-def to be (select-keys (realize-map defs)),
        ; but we do an extra internal select-keys inside of 
        ; realize-map in order to avoid evaling any default
        ; fields that we don't need
        ; The reason we add a (make model..) onto this is because
        ; we want to handle the case where the default is of the 
        ; wrong type - this will convert it
        part-defs (select-keys 
                     (parse model
                           (realize-map request (select-keys defs parts)))
                     parts)
        ; We want to get a parsed version of any of the partitions
        ; that the user passed as input - that's part-inp
        parsed (parse model input)
        supplied-parts (clojure.set/intersection 
                          (set parts) 
                          (set (keys input)))
        part-inp (select-keys parsed supplied-parts)
        ; The query-paras are the defaults and input of the partitions
        query-paras (merge part-defs part-inp)]
    (with-action-dsl request
      {listkee (query model query-paras)})))

(defn crud-model-new [request]
  (let [model (get-model request) 
        defaults (field-defaults model)]
    (realize-map request defaults)))


(defn crud-model-add [request]
  (let [model (get-model request)
        input (?! request :input)
        defs (parse model (realize-map request (field-defaults model)))
        combined (merge defs input)]
    (with-action-dsl request
      (-> (insert model combined)
          (with-alert :success "Saved")))))

(defn crud-model-show [request]
  (let [model (get-model request)
        keyfield (keyname model)]
    (with-action-dsl request
      (fetch model (?in keyfield)))))

(defn crud-model-edit [request]
  (let [model-sym (? request :input-mold-sym)
        model @(resolve model-sym)]
    (with-action-dsl request
      (fetch model (?in (keyname model))))))

(defn crud-model-alter [request]
  (let [model (get-model request)]
    (with-action-dsl request 
      ; Ensure we've got the key
      (?in (keyname model))
      (-> (save model input)
          (with-alert :success "Saved")))))

(defn crud-model-delete [request]
  (let [model (get-model request)
        keyfield (keyname model)]
    (with-action-dsl request
      (delete model (?in keyfield))
      nil)))


(defmacro def-crud-actions [model-sym]
  (let [model @(resolve model-sym)
        modelname (tablename model)]
    `(do
       (defn ~(symbol (str "list-" modelname)) [request#]
         (crud-model-list request#));~model-sym ~'system ~'input))
       (defn ~(symbol (str "new-" modelname)) [request#]
         (crud-model-new request#));~model-sym ~'system ~'input))
       (defn ~(symbol (str "add-" modelname)) [request#]
         (crud-model-add request#));~model-sym ~'system ~'input))
       (defn ~(symbol (str "show-" modelname)) [request#]
         (crud-model-show request#));~model-sym ~'system ~'input))
       (defn ~(symbol (str "edit-" modelname)) [request#]
         (crud-model-edit request#));~model-sym ~'system ~'input))
       (defn ~(symbol (str "alter-" modelname)) [request#]
         (crud-model-alter request#));~model-sym ~'system ~'input))
       (defn ~(symbol (str "delete-" modelname)) [request#]
         (crud-model-delete request#));~model-sym ~'system ~'input)))))
       )))


(defn with-lookup [request m model]
  (let [model (get-model request)
        action-name (str "list-" (tablename model))
        actionfinder (? request [:system :action-finder])
        af-request (assoc request :params {:_action action-name})
        action-sym (find-action actionfinder af-request)
        action @(resolve action-sym)
        lookup-res (action request)]
    (merge lookup-res m)))

(defmacro with-crud-dsl [request & forms]
  `(let [~'model (get-model ~request)
         ~'with-lookup (partial with-lookup ~request)]
     ~@forms))
