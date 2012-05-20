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
        [alida.test.helpers
         :only [with-test-db +test-db+ +test-server+ dummy-routes]]
        [alida.db] :reload)
  (:require [clojure.data.json :as json]
            [clj-http.client :as http-client]
            [com.ashafa.clutch :as clutch]
            [alida.util :as util]
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
  (with-fake-routes dummy-routes
    (with-redefs [util/make-timestamp #(str "2012-05-13T21:52:58.114Z")]
      @(crawl/directed-crawl
        "fake-routes"
        "2012-05-13T21:52:58.114Z"
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
  (let [{:keys [_id
                _rev
                score
                crawled-at
                uri
                type
                crawl-tag
                crawl-timestamp
                headers
                body]}
        (store-page +test-db+
                    "store-page-test"
                    "2012-05-13T21:52:58.114Z"
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
    (is (= crawl-timestamp "2012-05-13T21:52:58.114Z"))
    (is (= headers {:foo "bar"}))
    (is (= body "..."))))

(deftest test-get-page
  (do
    (create-views +test-db+))
  
  (let [p1 (store-page +test-db+
                       "get-page-test"
                       "2012-05-13T21:52:58.114Z"
                       "http://www.vixu.com/"
                       {:headers {:foo "bar"}
                        :body "p1"}
                       1)
        p2 (store-page +test-db+
                       "get-page-test"
                       "2012-05-13T21:52:58.114Z"
                       "http://www.vixu.com/"
                       {:headers {:foo "bar"}
                        :body "p2"}
                       1)
        p3 (store-page +test-db+
                       "get-page-test"
                       "2012-05-13T21:52:58.114Z"
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

(deftest test-get-pages-for-crawl-tag-and-timestamp
  (create-views +test-db+)
  (let [[p1a p2a p3a p4a p5a p6a p7a p8a p9a p10a]
        (map #(store-page +test-db+
                          "get-pages-for-crawl-tag-and-timestamp-test"
                          "2012-05-13T21:52:58.114Z"
                          (str "http://www.vixu.com/p" (inc %))
                          {:headers {:foo "bar"}
                           :body (str "p" (inc %))}
                          1)
             (range 10))
        [p1b p2b p3b p4b p5b p6b p7b p8b p9b p10b]
        (map #(store-page +test-db+
                          "get-pages-for-crawl-tag-and-timestamp-test"
                          "2012-05-19T04:18:50.678Z"
                          (str "http://www.vixu.com/p" (inc %))
                          {:headers {:foo "bar"}
                           :body (str "p" (inc %))}
                          1)
             (range 10))]

    (is (= (count
            (:documents
             (get-pages-for-crawl-tag-and-timestamp
              +test-db+
              "get-pages-for-crawl-tag-and-timestamp-test"
              "2012-05-13T21:52:58.114Z"
              100)))
           10))

    (is (= (count
            (:documents
             (get-pages-for-crawl-tag-and-timestamp
              +test-db+
              "get-pages-for-crawl-tag-and-timestamp-test"
              "2012-05-19T04:18:50.678Z"
              100)))
           10))

    (let [pages-a-result-1 (get-pages-for-crawl-tag-and-timestamp
                            +test-db+
                            "get-pages-for-crawl-tag-and-timestamp-test"
                            "2012-05-13T21:52:58.114Z"
                            5)]
      (is (= (count (:documents pages-a-result-1)) 5))
      (is (= (sort-by :uri (:documents pages-a-result-1))
             [p5a p6a p7a p8a p9a]))
      (is (= (:next pages-a-result-1) {:uri (:uri p4a)
                                       :timestamp (:crawled-at p4a)
                                       :id (:_id p4a)}))

      (let [pages-a-result-2 (get-pages-for-crawl-tag-and-timestamp
                              +test-db+
                              "get-pages-for-crawl-tag-and-timestamp-test"
                              "2012-05-13T21:52:58.114Z"
                              5
                              (:uri (:next pages-a-result-1))
                              (:timestamp (:next pages-a-result-1))
                              (:id (:next pages-a-result-1)))]
        (is (= (count (:documents pages-a-result-2)) 5))
        (is (= (sort-by :uri (:documents pages-a-result-2))
               [p1a p10a p2a p3a p4a]))
        (is (= (:next pages-a-result-2) nil))))))

(deftest test-get-scrape-results
  (create-views +test-db+)
  (let [dummy-scrape
        (map (fn [n]
               {:type "scrape-result"
                :uri (str "http://www.vixu.com/" n)
                :crawled-at "2012-05-19T23:30:09.021Z"
                :crawl-tag "get-scrape-results-test"
                :crawl-timestamp "2012-05-19T04:18:50.678Z"
                :title (str "Page " (inc n))
                :fulltext (str n " is an interesting number!")})
             (range 10))
        old-dummy-scrape
        (map (fn [n]
               {:type "scrape-result"
                :uri (str "http://www.vixu.com/" n)
                :crawled-at "2012-05-19T23:30:09.021Z"
                :crawl-tag "get-scrape-results-test"
                :crawl-timestamp "2012-05-18T04:18:50.678Z"
                :title (str "Page " (inc n))
                :fulltext (str n " is an interesting number!")})
             (range 10))
        dummy-docs (map (fn [x {:keys [id rev]}]
                          (merge x {:_id id :_rev rev}))
                        dummy-scrape
                        (add-batched-documents +test-db+ dummy-scrape))]

    (do
      (add-batched-documents +test-db+ old-dummy-scrape))
    
    (is (= (count
            (:documents
             (get-scrape-results
              +test-db+
              "get-scrape-results-test"
              "2012-05-19T04:18:50.678Z"
              100)))
           10))

    (is (= (count
            (:documents
             (get-scrape-results
              +test-db+
              "get-scrape-results-test"
              "2012-05-18T04:18:50.678Z"
              100)))
           10))

    (let [scrape-results-a (get-scrape-results
                            +test-db+
                            "get-scrape-results-test"
                            "2012-05-19T04:18:50.678Z"
                            5)]
      (is (= (count (:documents scrape-results-a)) 5))
      
      (is (=  (:documents scrape-results-a)
              (take 5 (reverse dummy-docs))))
      
      (is (= (:next scrape-results-a)
             {:uri (:uri (nth dummy-docs 4))
              :id (:_id (nth dummy-docs 4))}))

      (let [scrape-results-b (get-scrape-results
                              +test-db+
                              "get-scrape-results-test"
                              "2012-05-19T04:18:50.678Z"
                              5
                              (:uri (:next scrape-results-a))
                              (:id (:next scrape-results-a)))]
        (is (= (count (:documents scrape-results-b)) 5))
      
        (is (=  (:documents scrape-results-b)
                (take-last 5 (reverse dummy-docs))))
      
        (is (= (:next scrape-results-b) nil))))))