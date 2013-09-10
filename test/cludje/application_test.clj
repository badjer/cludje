(ns cludje.application-test
  (:use midje.sweet
        cludje.system
        cludje.model
        cludje.types
        cludje.test
        cludje.application))

(def cog (>Model {:name Str} {:modelname "cog"}))

(defn new-cog [request] (assoc request :output {:name "A"}))

(def system-args {:action-namespaces ['cludje.application-test] 
                  :mold-namespaces ['cludje.application-test]})

(fact ">api-pipeline"
  (let [system (>test-system system-args)
        ap (>api-pipeline system)]
  (fact "works end-to-end" 
    (ap {:params {:_action :new-cog}}) => (contains {:result {:name "A"}}))))

