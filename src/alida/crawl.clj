(ns alida.crawl
  (:require [clj-http.client :as http-client]
            [clj-http.cookies :as cookies]
            [net.cgrand.enlive-html :as enlive]))

(defn get-page [uri]
  (http-client/get uri))