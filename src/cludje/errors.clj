(ns cludje.errors)

(defn throw-problems 
  ([]
   (throw-problems {}))
  ([problems]
   (throw (ex-info (str "Problems: " problems)
                   {:__problems problems}))))

(defn throw-unauthorized 
  ([] (throw-unauthorized {}))
  ([details]
    (throw (ex-info (str "Unauthorized: " details)
                    (merge {:__unauthorized "Unauthorized"} details)))))

(defn throw-not-found 
  ([] (throw-not-found {}))
  ([details]
    (throw (ex-info (str "Not found: " details)
                    (merge {:__notfound "Not found"} details)))))

(defn throw-not-logged-in 
  ([] (throw-not-logged-in {}))
  ([details]
    (throw (ex-info (str "Not logged in: " details)
                    (merge {:__notloggedin "Not logged in"} details)))))

(defn throw-error
  ([] (throw-error {}))
  ([details]
   (throw (ex-info (str "System error: " details)
                   (merge {:__systemerror "System Error"} details)))))
