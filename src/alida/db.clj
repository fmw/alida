;; src/alida/db.clj: database functions
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

;; Note that database usage could be optimized and it is worth looking
;; into storing the result HTML as compressed attachments, for
;; example. CouchDB is not really a good fit in the first place when
;; you actually need those optimizations, so I decided to keep the
;; code as simple as possible. Consider porting to Apache HBase/HDFS
;; if you're planning on using something like this in production.

(ns alida.db
  (:require [com.ashafa.clutch :as clutch]
            [alida.util :as util]))

(defn create-views [database]
  "Adds a design document called 'views' with the required views to
   the CouchDB database."
  (clutch/with-db database
    (clutch/save-view
     "views"
     (clutch/view-server-fns
      :cljs
      {:pages {:map (fn [doc]
                      (if (= (aget doc "type") "crawled-page")
                        (js/emit (to-array
                                  [(aget doc "uri") (aget doc "crawled-at")])
                                 doc)))}}))))

(defn add-batched-documents [database documents]
  "Adds provided documents to database."
  (clutch/bulk-update database documents))

(defn store-page
  "Stores the given request object in CouchDB with the provided uri
  and score values."
  [database uri request score]
  (clutch/put-document database
                       (assoc request
                         :type "crawled-page"
                         :uri uri
                         :crawled-at (util/make-timestamp)
                         :score score)))

(defn get-page-history
  "Returns limit versions of the crawl history for the page with the
   provided uri in the provided database."
  [database uri limit]
  (let [{:keys [scheme host path]} (util/get-uri-segments uri)]
    (map :value
         (clutch/get-view database
                          "views"
                          "pages"
                          {:startkey [uri {}]
                           :endkey [uri]
                           :limit limit
                           :descending true}))))

(defn get-page
  "Returns the most recently crawed page for uri in database."
  [database uri]
  (first (get-page-history database uri 1)))