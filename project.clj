(defproject alida "0.1.0-SNAPSHOT"
  :description "Crawling, scraping and indexing application."
  :url "https://github.com/fmw/alida"
  :license {:name "Apache License, version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/clojurescript "0.0-1011"]
                 [org.clojure/data.json "0.1.2"]
                 [clj-http "0.4.0"]
                 [enlive "1.0.0-SNAPSHOT"]
                 [org.jsoup/jsoup "1.6.1"]
                 [com.ashafa/clutch "0.4.0-SNAPSHOT"]
                 [clj-time "0.3.7"]
                 [org.apache.lucene/lucene-core "4.0-SNAPSHOT"]
                 [org.apache.lucene/lucene-queryparser "4.0-SNAPSHOT"]
                 [org.apache.lucene/lucene-analyzers-common "4.0-SNAPSHOT"]
                 [clj-http-fake "0.3.0"]]
  :exclusions [lein-swank swank-clojure]
  :repositories {"apache-snapshots"
                 "https://repository.apache.org/content/groups/snapshots/"})