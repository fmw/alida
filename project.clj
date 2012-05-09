(defproject alida "0.1.0-SNAPSHOT"
  :description "Crawling, scraping and indexing application."
  :url "https://github.com/fmw/alida"
  :license {:name "Apache License, version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-http "0.4.0"]
                 [enlive "1.0.0-SNAPSHOT"]
                 [org.jsoup/jsoup "1.6.1"]
                 [com.ashafa/clutch "0.4.0-SNAPSHOT"]]
  :dev-dependencies [[clj-http-fake "0.3.0"]
                     [radagast "1.1.0"]]
  :plugins [[lein-swank "1.4.4"]])