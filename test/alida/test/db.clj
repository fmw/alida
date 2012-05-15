;; test/alida/test/db.clj: tests for database functions
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

(ns alida.test.db
  (:use [alida.db] :reload
        [clojure.test]
        [clj-http.fake])
  (:require [clojure.data.json :as json]
            [clj-http.client :as http-client]
            [com.ashafa.clutch :as clutch]
            [alida.test.crawl :as test-crawl]
            [alida.crawl :as crawl]))

(defn couchdb-id? [s]
  (re-matches #"^[a-z0-9]{32}$" s))

(defn couchdb-rev?
  ([s]
     (couchdb-rev? 1 s))
  ([rev-num s]
     (re-matches (re-pattern (str "^" rev-num "-[a-z0-9]{32}$")) s)))

(defn iso-date? [s]
  (re-matches
   #"^[\d]{4}-[\d]{2}-[\d]{2}T[\d]{2}:[\d]{2}:[\d]{2}\.[\d]{1,4}Z"
   s))

(defn random-lower-case-string [length]
  ;; to include uppercase
  ;; (let [ascii-codes (concat (range 48 58) (range 66 91) (range 97 123))])
  (let [ascii-codes (concat (range 48 58) (range 97 123))]
    (apply str (repeatedly length #(char (rand-nth ascii-codes))))))

(def dummy-crawled-pages
  (with-fake-routes test-crawl/dummy-routes
    (with-redefs [crawl/make-timestamp #(str "2012-05-13T21:52:58.114Z")]
      @(crawl/directed-crawl
        0
        "http://www.dummyhealthfoodstore.com/index.html"
        [{:selector [:ul#menu :a]
          :path-filter #"^/products.+"
          :next [{:selector [[:div#content] [:a]]}]}]))))

(def +test-db+ (str "alida-test-" (random-lower-case-string 20)))
(def +test-server+ "http://localhost:5984/")

(defn database-fixture [f]
  (clutch/get-database +test-db+)
  (f)
  (clutch/delete-database  +test-db+))

(use-fixtures :each database-fixture)

(deftest test-create-views
  (is (= (create-views +test-db+)
         (json/read-json (:body
                          (http-client/get
                           (str +test-server+
                                +test-db+
                                "/_design/views")))))))

(deftest test-add-batched-documents
  (is (= (:doc_count (clutch/database-info +test-db+)) 0))

  (do
    (add-batched-documents +test-db+ dummy-crawled-pages))

  (is (= (:doc_count (clutch/database-info +test-db+)) 13)))