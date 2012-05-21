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
            [clj-time.core :as time-core]
            [alida.util :as util]
            [alida.db :as db]
            [alida.scrape :as scrape]))

(defn crawled-in-last-hour?
  "Returns false if the :crawled-at value for the provided document
   map is either nil (which happens when the document isn't found) or
   at least 30 minutes ago."
  [{:keys [crawled-at]}]
  (if (nil? crawled-at)
    false
    (time-core/after?
     (util/rfc3339-to-jodatime crawled-at "GMT")
     (time-core/minus (time-core/now) (time-core/minutes 60)))))

(defn get-page
  "Wrapper around clj-http.client/get fn that requests the provided
   uri and supresses any exceptions (will return nil if something goes
   wrong)."
  [uri]
  (try
    (http-client/get uri)
    (catch Exception e
      nil)))

(defn same-domain?
  "Returns true if the provided URIs share the same domain."
  [& uris]
  (apply =
         (map (fn [uri]
                (:host (util/get-uri-segments uri)))
              uris)))

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

;;TODO: implement support for e.g. 404

(defn directed-crawl
  "Crawls a set of pages, given a crawl-tag and crawl-timestamp to
   distinguish the particular crawl job, sleep-for (delay between
   requests in milliseconds), a seed-uri string (e.g.
   http://www.vixu.com/) and a sequence of selectors to extract
   links to other pages that are to be crawled with. Runs in a
   separate thread, through a future.  Returns a sequence of clj-http
   request maps, with added keys for the uri (:uri), a crawl
   timestamp (:crawled-at) and the document :type (crawled-page).

   Each selector in the selectors sequence is a map with at least a
   :selector key mapped to an Enlive selector vector. The :filter key
   is optional and maps to a regular expression pattern that is
   matched against the links to filter them. The :path-filter key is
   similar to the :filter key, but is only matched against the path
   segment of the links. The :next key maps to the sequence of
   selectors that is used to extract links from pages matched by the
   current selector.

   The directed-crawl fn is a good match for a relatively small,
   targeted crawl against a single hostname. Call separately for
   different hostnames. This function isn't optimal for exploratory
   crawling or gathering large sets of pages (e.g. the whole of
   Wikipedia), because of the potentially huge results sequence
   that would generate."
  [crawl-tag crawl-timestamp sleep-for seed-uri initial-selectors]
  (future
    (loop [todo-links [{:uri seed-uri :selectors initial-selectors}]
           crawled-links #{}
           results []]
      (if-let [{:keys [uri selectors]} (first todo-links)]
        (if (contains? crawled-links uri)
          (recur (rest todo-links) crawled-links results)
          (let [req (get-page uri)]
            (Thread/sleep sleep-for)
            (recur (concat (rest todo-links)
                           (get-crawlable-links-for-document uri
                                                             req
                                                             selectors))
                   (conj crawled-links uri)
                   (conj results (assoc req
                                   :type "crawled-page"
                                   :crawl-tag crawl-tag
                                   :crawl-timestamp crawl-timestamp
                                   :uri uri
                                   :crawled-at (util/make-timestamp))))))
        results))))

(defn weighted-crawl
  "Crawling fn that stores results tagged with the crawl-tag and
   crawl-timestamp directly into the provided database, pauses between
   requests to the same host for sleep-for seconds and starts with the
   seed-uri. Stops going deeper when max-depth is reached, with the
   seed-uri being depth level 0. The page-scoring-fn should accept the
   active uri and clj-http request map as its only arguments and
   determines how the crawler will treat the active page. The
   page-scoring-fn should return a positive number if it is relevant,
   zero if it isn't interesting enough to store but still worth
   following links from and a negative number if it should be ignored
   altogether. Also accepts a link-checker-fn as an optional fifth
   argument, which should return a boolean value or nil. The
   link-checker-fn is useful to filter the crawling process on a URI
   basis (e.g. to limit the crawl to a specific domain or subset of
   pages with a marker in the URI to check for).

   This fn is a starting point for a more scalable crawling mechanism.
   It spawns a new thread for every domain that is being
   crawled (unlike the current implementation of the directed-crawl
   fn). It doesn't have a return value and inserts a separate document
   into CouchDB for every accepted page. This is useful for a
   long-running process, but means taking a performance hit because
   CouchDB is much faster at bulk inserts. Alternatively, this fn
   could be rewritten to cache e.g. 1000 results and store those
   through a bulk insert instead. Another optimization would be to
   keep a collection of crawled URIs in an atom and check against
   that, to avoid having to look up the last crawled document in the
   database (currently, the crawled-links don't carry over when a new
   instance of the weighted-crawl function is called when it finds a
   link to another domain). A large scale solution would use either
   Apache Nutch or a custom implementation on top of e.g. Apache
   Hadoop and HDFS/HBase, but the essential implementation would
   be similar to what this function does."
  [database
   crawl-tag
   crawl-timestamp
   sleep-for
   seed-uri
   max-depth
   page-scoring-fn
   & [link-checker-fn]]
  (future
    (loop [todo-links [[seed-uri 0]]
           crawled-links #{}]
      (when-let [[active-uri depth] (first todo-links)]
        (if (and (not (contains? crawled-links active-uri))
                 (<= depth max-depth)
                 (or (not link-checker-fn) (link-checker-fn active-uri))
                 (not (crawled-in-last-hour? (db/get-page database
                                                          crawl-tag
                                                          active-uri))))
          (if (same-domain? active-uri seed-uri)
            (if-let [req (get-page active-uri)]
              (do
                (Thread/sleep sleep-for)
                (let [score (page-scoring-fn active-uri depth req)]
                  (when (pos? score)
                    (db/store-page database
                                   crawl-tag
                                   crawl-timestamp
                                   active-uri
                                   req
                                   score))
                  (recur (if (neg? score)
                           (rest todo-links)
                           (concat (rest todo-links)
                                   (map (fn [uri]
                                          [uri (inc depth)])
                                    (scrape/get-links-jsoup active-uri
                                                            (:body req)))))
                         (conj crawled-links active-uri))))
              (recur (rest todo-links) (conj crawled-links active-uri)))
            (do
              (weighted-crawl database
                              crawl-tag
                              crawl-timestamp
                              sleep-for
                              active-uri
                              max-depth
                              page-scoring-fn
                              link-checker-fn)
              (recur (rest todo-links) (conj crawled-links active-uri))))
          (recur (rest todo-links) (conj crawled-links active-uri)))))))