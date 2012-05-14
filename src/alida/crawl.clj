;; src/alida/crawl.clj: crawling functions
;;
;; Copyright 2012, F.M. de Waard & Vixu.com <fmw@vixu.com>.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;; 
;; http://www.apache.org/licenses/LICENSE-2.0
;; 
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns alida.crawl
  (:require [clj-http.client :as http-client]
            [clj-http.cookies :as cookies]
            [clj-time.format :as time-format]
            [clj-time.core :as time-core]
            [alida.scrape :as scrape]))

(defn make-timestamp
  "Returns a string with the RFC 3339 timestamp for the current date/time.
   e.g. 2012-05-13T21:52:58.114Z"
  []
  (time-format/unparse
   (time-format/formatters :date-time)
   (time-core/now)))

(defn get-links-for-selector
  [base-uri html {:keys [selector filter path-filter next]}]
  (map #(zipmap [:uri :selectors] [% next])
       (scrape/get-links base-uri
                         html
                         selector
                         {:filter filter
                          :path-filter path-filter})))

(defn get-crawlable-links-for-document
  "Expects a base-uri (the currently crawled page; to determine prefix
   in the absolute uri), an request map containing a :body keyword
   mapping to a string with the HTML for the page, and a sequence of
   selector maps. Each selector map needs to have a :selector key with
   an Enlive selector for the desired links. Optional keys
   are :filter (with a regular expression pattern to filter uri
   strings) and :next (a sequence of selectors to run on pages matched
   by this selector).

   Returns a sequence of maps with :uri containing an absolute uri for
   a link and :selectors containing the selectors to gather links with
   on the subsequent page the :uri links to."
  [base-uri page-request-map selectors]
  (flatten
   (map (partial get-links-for-selector
                 base-uri
                 (:body page-request-map))
        selectors)))

(defn directed-crawl
  "Crawls a set of pages, given sleep-for (delay between requests in
   milliseconds), a seed-uri string (e.g. http://www.vixu.com/) and a
   sequence of selectors to extract links to other pages that are to
   be crawled with. Runs in a separate thread, through a future.
   Returns a sequence of clj-http request maps, with added keys for
   the uri (:uri) and a crawl timestamp (:crawled-at).

   Each selector in the selectors sequence is a map with at least a
   :selector key mapped to an Enlive selector vector. The :filter
   key is optional and maps to a regular expression pattern that
   is matched against the links to filter them. The :next key maps
   to the sequence of selectors that is used to extract links from
   pages matched by the current selector.

   The directed-crawl fn is a good match for a relatively small,
   targeted crawl against a single hostname. Call separately for
   different hostnames. This function isn't optimal for exploratory
   crawling or gathering large sets of pages (e.g. the whole of
   Wikipedia), because of the potentially huge results sequence
   that would generate."
  [sleep-for seed-uri initial-selectors]
  (future
    (loop [todo-links [{:uri seed-uri :selectors initial-selectors}]
           crawled-links #{}
           results []]
      (if-let [{:keys [uri selectors]} (first todo-links)]
        (if (contains? crawled-links uri)
          (recur (rest todo-links) crawled-links results)
          (let [req (http-client/get uri)]
            (Thread/sleep sleep-for)
            (recur (concat (rest todo-links)
                           (get-crawlable-links-for-document uri
                                                             req
                                                             selectors))
                   (conj crawled-links uri)
                   (conj results (assoc req
                                   :uri uri
                                   :crawled-at (make-timestamp))))))
        results))))