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
  (:use [clojure.test]
        [clj-http.fake]
        [alida.test.helpers :only [with-test-db +test-db+ +test-server+]]
        [alida.db] :reload)
  (:require [clojure.data.json :as json]
            [clj-http.client :as http-client]
            [com.ashafa.clutch :as clutch]
            [alida.util :as util]
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

(defn database-fixture [f]
  (with-test-db (f)))

(use-fixtures :each database-fixture)

(deftest test-create-views
  (is (= (create-views +test-db+)
         (json/read-json (:body
                          (http-client/get
                           (str +test-server+
                                +test-db+
                                "/_design/views")))))))

(def dummy-crawled-pages
  (with-fake-routes test-crawl/dummy-routes
    (with-redefs [util/make-timestamp #(str "2012-05-13T21:52:58.114Z")]
      @(crawl/directed-crawl
        "fake-routes"
        0
        "http://www.dummyhealthfoodstore.com/index.html"
        [{:selector [:ul#menu :a]
          :path-filter #"^/products.+"
          :next [{:selector [[:div#content] [:a]]}]}]))))

(deftest test-add-batched-documents
  (is (= (:doc_count (clutch/database-info +test-db+)) 0))

  (do
    (add-batched-documents +test-db+ dummy-crawled-pages))

  (is (= (:doc_count (clutch/database-info +test-db+)) 13)))

(deftest test-store-page
  (let [{:keys [_id _rev score crawled-at uri type crawl-tag headers body]}
        (store-page +test-db+
                    "store-page-test"
                    "http://www.vixu.com/"
                    {:headers {:foo "bar"}
                     :body "..."}
                    1.0)]
    (is (couchdb-id? _id))
    (is (couchdb-rev? _rev))
    (is (= score 1.0))
    (is (iso-date? crawled-at))
    (is (= uri "http://www.vixu.com/"))
    (is (= type "crawled-page"))
    (is (= crawl-tag "store-page-test"))
    (is (= headers {:foo "bar"}))
    (is (= body "..."))))

(deftest test-get-page
  (do
    (create-views +test-db+))
  
  (let [p1 (store-page +test-db+
                       "get-page-test"
                       "http://www.vixu.com/"
                       {:headers {:foo "bar"}
                        :body "p1"}
                       1)
        p2 (store-page +test-db+
                       "get-page-test"
                       "http://www.vixu.com/"
                       {:headers {:foo "bar"}
                        :body "p2"}
                       1)
        p3 (store-page +test-db+
                       "get-page-test"
                       "http://www.vixu.com/en/pricing.html"
                       {:headers {:foo "bar"}
                        :body "p3"}
                       1)]

    (is (= (get-page +test-db+
                     "get-page-test"
                     "http://www.vixu.com/")
           p2))

    (is (= (vec (get-page-history +test-db+
                                  "get-page-test"
                                  "http://www.vixu.com/"
                                  10))
           [p2 p1]))))