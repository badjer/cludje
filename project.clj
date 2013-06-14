(defproject cludje "0.1.0-SNAPSHOT"
  :description "Cludje - A clojure web framework"
  :url "http://github.com/badjer/cludje"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.novemberain/monger "1.5.0"]
                 [com.cemerick/friend "0.1.5"]
                 [compojure "1.1.5"]
                 [ring "1.1.8"]
                 [ring/ring-jetty-adapter "1.1.8"]
                 [hiccup "1.0.3"]]
  :source-paths ["src"]
  :test-paths ["test"]
  :main cludje.app
  :profiles {:dev {:dependencies [[lazytest "1.2.3"]
                                  [midje "1.5.0"]]
                   :plugins [[lein-midje "3.0.0"]]}})
