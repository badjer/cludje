(defproject cludje "0.5.17"
  :description "Cludje - A clojure web framework"
  :url "http://github.com/badjer/cludje"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.novemberain/monger "1.5.0"]
                 [com.cemerick/friend "0.1.5"]
                 [cheshire "5.2.0"]
                 [clj-http "0.7.2"]
                 [ring "1.1.8"]
                 [clj-time "0.6.0"]
                 [ring/ring-jetty-adapter "1.1.8"]
                 [ring/ring-json "0.2.0"]]
  :source-paths ["src"]
  :test-paths ["test"]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.3"] 
                                  [midje "1.5.1"]]
                   :source-paths ["test" "dev"]
                   :plugins [[lein-midje "3.0.1"]]}})
