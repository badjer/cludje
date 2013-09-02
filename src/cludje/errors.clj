(ns cludje.errors)

(defn throw-problems 
  ([]
   (throw-problems {}))
  ([problems]
   (throw (ex-info "Problems" {:__problems problems}))))

(defn throw-unauthorized 
  ([] (throw-unauthorized {}))
  ([details]
    (throw (ex-info "Unauthorized" 
                    (merge {:__unauthorized "Unauthorized"} details)))))

(defn throw-not-found 
  ([] (throw-not-found {}))
  ([details]
    (throw (ex-info "Not found" 
                    (merge {:__notfound "Not found"} details)))))

(defn throw-not-logged-in 
  ([] (throw-not-logged-in {}))
  ([details]
    (throw (ex-info "Not logged in" 
                    (merge {:__notloggedin "Not logged in"} details)))))

(defn throw-error
  ([] (throw-error {}))
  ([details]
   (throw (ex-info "System error"
                   (merge {:__systemerror "System Error"} details)))))
