(ns cludje.crud
  (:use cludje.action 
        cludje.util
        cludje.system
        cludje.model
        cludje.mold
        cludje.types))

(def crud-actions [:new :add :show :edit :alter :delete :list])

; (defn global-dologin [context]
;   (with-action-dsl context
;     (log-in context)))
; 
; (defn global-login [context]
;   {})
; 
; (defn global-logout [context]
;   (with-action-dsl context
;     (log-out context)))
; 
; (defn global-data [context]
;   (with-action-dsl context
;     {:user (current-user)
;      :menu [{:text "A" :url "/a"} {:text "B" :url "/b"}]
;      :footer "© 2013 Cludje"
;      :title "Cludje"}))
; 
(defn realize-one [context v]
  "Try to get a final-value for v.
  If it's a var, deref it. If it's a fn, call it with [system input]"
  (cond
    (fn? v) (v context)
    (var? v) (realize-one context @v)
    :else v))

(defn realize-map [context m]
  (into {} (for [[k v] m] [k (realize-one context v)])))

(defn get-model [context]
  (let [model-sym (? context :input-mold-sym)]
    @(resolve model-sym)))


(defn crud-model-list [context]
  (let [model (get-model context)
        input (?? context :input)
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
                           (realize-map context (select-keys defs parts)))
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
    (with-action-dsl context
      {listkee (query model query-paras)})))

(defn crud-model-new [context]
  (let [model (get-model context) 
        defaults (field-defaults model)]
    (realize-map context defaults)))

(defn crud-model-add [context]
  (let [model (get-model context)
        input (?? context :input)
        defs (parse model (realize-map context (field-defaults model)))
        combined (merge defs input)]
    (with-action-dsl context
      (-> (insert model combined)
          (with-alert :success "Saved")))))

(defn crud-model-show [context]
  (let [model (get-model context)
        keyfield (keyname model)]
    (with-action-dsl context
      (fetch model (?in keyfield)))))

(defn crud-model-edit [context]
  (let [model-sym (? context :input-mold-sym)
        model @(resolve model-sym)]
    (with-action-dsl context
      (fetch model (?in (keyname model))))))

(defn crud-model-alter [context]
  (let [model (get-model context)]
    (with-action-dsl context 
      ; Ensure we've got the key
      (?in (keyname model))
      (-> (save model input)
          (with-alert :success "Saved")))))

(defn crud-model-delete [context]
  (let [model (get-model context)
        keyfield (keyname model)]
    (with-action-dsl context
      (delete model (?in keyfield))
      nil)))


(defmacro def-crud-actions [model-sym]
  (let [model @(resolve model-sym)
        modelname (tablename model)]
    `(do
       (defn ~(symbol (str "list-" modelname)) [context#]
         (crud-model-list context#));~model-sym ~'system ~'input))
       (defn ~(symbol (str "new-" modelname)) [context#]
         (crud-model-new context#));~model-sym ~'system ~'input))
       (defn ~(symbol (str "add-" modelname)) [context#]
         (crud-model-add context#));~model-sym ~'system ~'input))
       (defn ~(symbol (str "show-" modelname)) [context#]
         (crud-model-show context#));~model-sym ~'system ~'input))
       (defn ~(symbol (str "edit-" modelname)) [context#]
         (crud-model-edit context#));~model-sym ~'system ~'input))
       (defn ~(symbol (str "alter-" modelname)) [context#]
         (crud-model-alter context#));~model-sym ~'system ~'input))
       (defn ~(symbol (str "delete-" modelname)) [context#]
         (crud-model-delete context#));~model-sym ~'system ~'input)))))
       )))


(defn with-lookup [context m model]
  (let [model (get-model context)
        action-name (str "list-" (tablename model))
        actionfinder (? context [:system :action-finder])
        af-context (assoc context :parsed-input {:_action action-name})
        action-sym (find-action actionfinder af-context)
        action @(resolve action-sym)
        lookup-res (action context)]
    (merge lookup-res m)))

(defmacro with-crud-dsl [context & forms]
  `(let [~'model (get-model ~context)
         ~'with-lookup (partial with-lookup ~context)]
     ~@forms))
