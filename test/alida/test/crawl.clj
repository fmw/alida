;; test/alida/test/crawl.clj: tests for crawling functions
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

(ns alida.test.crawl
  (:use [clojure.test]
        [clj-http.fake]
        [alida.test.helpers :only [with-test-db +test-db+ +test-server+]]
        [alida.crawl] :reload)
  (:require [clj-http.client :as http-client]
            [clj-time.core :as time-core]
            [clj-time.format :as time-format]
            [clj-time.coerce :as time-coerce]
            [com.ashafa.clutch :as clutch]
            [net.cgrand.enlive-html :as enlive]
            [alida.util :as util]
            [alida.db :as db])
  (:import [java.io StringReader]))

(defn get-filename-from-uri [uri]
  (last (clojure.string/split uri #"/")))

(defn dummy-response [file]
  (fn [req]
    {:status 200
     :headers {}
     :body (slurp file)}))

(def dummy-health-food-uris
  ["http://www.dummyhealthfoodstore.com/index.html"
   "http://www.dummyhealthfoodstore.com/products/whisky.html"
   "http://www.dummyhealthfoodstore.com/products/pipe-tobacco.html"
   "http://www.dummyhealthfoodstore.com/brora.html"
   "http://www.dummyhealthfoodstore.com/port-ellen.html"
   "http://www.dummyhealthfoodstore.com/macallan.html"
   "http://www.dummyhealthfoodstore.com/clynelish.html"
   "http://www.dummyhealthfoodstore.com/ardbeg.html"
   "http://www.dummyhealthfoodstore.com/glp-westminster.html"
   "http://www.dummyhealthfoodstore.com/glp-meridian.html"
   "http://www.dummyhealthfoodstore.com/blackwoods-flake.html"
   "http://www.dummyhealthfoodstore.com/navy-rolls.html"
   "http://www.dummyhealthfoodstore.com/london-mixture.html"])

(def dummy-deeply-nested-uris
  ["http://www.deeply-nested-dummy.com/index.html"
   "http://www.deeply-nested-dummy.com/nested-0a.html"
   "http://www.deeply-nested-dummy.com/nested-0b.html"
   "http://www.deeply-nested-dummy.com/nested-1a.html"
   "http://www.deeply-nested-dummy.com/nested-1b.html"
   "http://www.deeply-nested-dummy.com/nested-2a.html"
   "http://www.deeply-nested-dummy.com/nested-2b.html"
   "http://www.deeply-nested-dummy.com/nested-3a.html"
   "http://www.deeply-nested-dummy.com/nested-3b.html"
   "http://www.deeply-nested-dummy.com/nested-4a.html"
   "http://www.deeply-nested-dummy.com/nested-4b.html"
   "http://www.deeply-nested-dummy.com/nested-5a.html"
   "http://www.deeply-nested-dummy.com/nested-5b.html"
   "http://www.deeply-nested-dummy.com/nested-6a.html"
   "http://www.deeply-nested-dummy.com/nested-6b.html"
   "http://www.deeply-nested-dummy.com/nested-7a.html"
   "http://www.deeply-nested-dummy.com/nested-7b.html"
   "http://www.deeply-nested-dummy.com/nested-8a.html"
   "http://www.deeply-nested-dummy.com/nested-8b.html"
   "http://www.deeply-nested-dummy.com/nested-9a.html"
   "http://www.deeply-nested-dummy.com/nested-9b.html"])

(def dummy-routes
  (merge
   (zipmap dummy-health-food-uris
           (map #(dummy-response
                  (str "resources/test-data/dummy-shop/"
                       (get-filename-from-uri %)))
                dummy-health-food-uris))
   (zipmap dummy-deeply-nested-uris
           (map #(dummy-response (str "resources/test-data/deeply-nested/"
                                      (get-filename-from-uri %)))
                dummy-deeply-nested-uris))
   {"http://www.vixu.com/" (fn [req]
                             {:status 200
                              :headers {}
                              :body "Hic sunt dracones"})}))

(deftest test-crawled-in-last-hour?
  (is (false? (crawled-in-last-hour? nil)))
  (is (false? (crawled-in-last-hour? {:crawled-at nil})))
  (is (false? (crawled-in-last-hour?
               {:crawled-at "2012-02-17T05:58:49.591Z"})))
  (is (false? (crawled-in-last-hour?
               {:crawled-at (time-format/unparse
                             (time-format/formatters :date-time)
                             (time-core/minus (time-core/now)
                                              (time-core/minutes 61)))})))
  (is (true? (crawled-in-last-hour?
              {:crawled-at (time-format/unparse
                            (time-format/formatters :date-time)
                            (time-core/minus (time-core/now)
                                             (time-core/minutes 59)))}))))

(deftest test-get-page
  (with-fake-routes dummy-routes
    (are [uri]
         (= (get-page uri)
            {:trace-redirects [uri]
             :status 200
             :headers {}
             :body (slurp (str "resources/test-data/dummy-shop/"
                               (get-filename-from-uri uri)))})
         
         "http://www.dummyhealthfoodstore.com/index.html"
         "http://www.dummyhealthfoodstore.com/products/whisky.html"
         "http://www.dummyhealthfoodstore.com/products/pipe-tobacco.html"
         "http://www.dummyhealthfoodstore.com/brora.html"
         "http://www.dummyhealthfoodstore.com/port-ellen.html"
         "http://www.dummyhealthfoodstore.com/clynelish.html"
         "http://www.dummyhealthfoodstore.com/macallan.html"
         "http://www.dummyhealthfoodstore.com/ardbeg.html"
         "http://www.dummyhealthfoodstore.com/glp-westminster.html"
         "http://www.dummyhealthfoodstore.com/glp-meridian.html"
         "http://www.dummyhealthfoodstore.com/blackwoods-flake.html"
         "http://www.dummyhealthfoodstore.com/navy-rolls.html"
         "http://www.dummyhealthfoodstore.com/london-mixture.html"))

  (testing "expect nil instead of an exception"
    (is (= (get-page "foo") nil))))

(deftest test-same-domain?
  (is (same-domain? "http://www.dummyhealthfoodstore.com/navy-rolls.html"
                    "http://www.dummyhealthfoodstore.com/brora.html"
                    "http://www.dummyhealthfoodstore.com/foo.bar"))

  (is (not (same-domain? "http://www.vixu.com/"
                         "http://www.dummyhealthfoodstore.com/brora.html"
                         "http://www.dummyhealthfoodstore.com/foo.bar"))))

(deftest test-get-links-for-selector
  (with-fake-routes dummy-routes
    (testing "try grabbing all links from a page"
      (is (= (sort-by
              :uri
              (get-links-for-selector
               "http://www.dummyhealthfoodstore.com/glp-westminster.html"
               (:body
                (http-client/get
                 "http://www.dummyhealthfoodstore.com/glp-westminster.html"))
               {:selector [:a]}))
             (sort-by
              :uri
              [{:selectors nil
                :uri "http://www.dummyhealthfoodstore.com/en/About.html"}
               {:selectors nil
                :uri (str "http://www.dummyhealthfoodstore.com/"
                          "products/pipe-tobacco.html")}
               {:selectors nil
                :uri "http://www.dummyhealthfoodstore.com/nl"}
               {:selectors nil
                :uri "http://www.dummyhealthfoodstore.com/"}
               {:selectors nil
                :uri "http://www.dummyhealthfoodstore.com/en/Contact.html"}
               {:selectors nil
                :uri (str "http://www.dummyhealthfoodstore.com/"
                          "products/whisky.html")}]))))

    (testing "test uri filtering with a regular expression"
      (is (= (get-links-for-selector
              "http://www.dummyhealthfoodstore.com/glp-westminster.html"
              (:body
               (http-client/get
                "http://www.dummyhealthfoodstore.com/glp-westminster.html"))
              {:selector [:a]
               :path-filter #"^/products.+"})
             [{:selectors nil
               :uri (str "http://www.dummyhealthfoodstore.com/"
                         "products/pipe-tobacco.html")}
              {:selectors nil
               :uri (str "http://www.dummyhealthfoodstore.com/"
                         "products/whisky.html")}])))))

(deftest test-directed-crawl
  (with-fake-routes dummy-routes
    (with-redefs [util/make-timestamp #(str "2012-05-13T21:52:58.114Z")]
      (is
       (=
        (sort-by
         :uri
         @(directed-crawl "test-directed-crawl"
                          0
                          "http://www.dummyhealthfoodstore.com/index.html"
                          [{:selector [:ul#menu :a]
                            :path-filter #"^/products.+"
                            :next [{:selector [[:div#content] [:a]]}]}]))
        (sort-by
         :uri
         @(directed-crawl "test-directed-crawl"
                          0
                          "http://www.dummyhealthfoodstore.com/index.html"
                          [{:selector [:ul#menu :a]
                            :path-filter #"^/products.+"
                            :next [{:selector [[:div#content] [:a]]
                                    :next [{:selector [:ul#menu :a]
                                            :path-filter #"^/products.+"}]}
                                   {:selector [:ul#menu :a]
                                    :path-filter #"^/products.+"}]}]))
        (sort-by
         :uri
         (map (fn [uri]
                (zipmap [:type
                         :crawl-tag
                         :crawl-timestamp
                         :uri
                         :crawled-at
                         :trace-redirects
                         :status
                         :headers
                         :body]
                        ["crawled-page"
                         "test-directed-crawl"
                         "2012-05-13T21:52:58.114Z"
                         uri
                         "2012-05-13T21:52:58.114Z"
                         [uri]
                         200
                         {}
                         (slurp (str "resources/test-data/dummy-shop/"
                                     (get-filename-from-uri uri)))]))
              dummy-health-food-uris)))))

    (testing "make sure that sleep time is respected"
      (let [[first-page-crawled-at
             second-page-crawled-at
             third-page-crawled-at]
            (map #(time-format/parse (time-format/formatters :date-time) %)
                 (map
                  :crawled-at
                  @(directed-crawl
                    "test-directed-crawl"
                    100
                    "http://www.dummyhealthfoodstore.com/index.html"
                    [{:selector [:ul#menu :a]
                      :path-filter #"^/products/whisky.html"
                      :next [{:selector
                              [:ul#menu :a]
                              :path-filter
                              #"^/products/pipe-tobacco.html"}]}])))]
        (are [page-crawled-at-times]
             (> (- (time-coerce/to-long (second page-crawled-at-times))
                   (time-coerce/to-long (first page-crawled-at-times)))
                100)
             [first-page-crawled-at second-page-crawled-at]
             [second-page-crawled-at third-page-crawled-at])))))

(deftest test-weighted-crawl
  (with-fake-routes dummy-routes
    (testing "try a full crawl"
      (with-redefs [util/make-timestamp #(str "2012-05-13T21:52:58.114Z")]
        (with-test-db
          (db/create-views +test-db+)
          (let [page-scoring-fn
                (fn [uri request-data]
                  (float 0.1))
                link-checker-fn
                (fn [uri]
                  (not
                   (nil?
                    (re-matches #"http://www.deeply-nested.*" uri))))]
            @(weighted-crawl +test-db+
                             "test-weighted-crawl"
                             0
                             "http://www.deeply-nested-dummy.com/index.html"
                             page-scoring-fn
                             link-checker-fn)

            (let [results (map :value
                               (clutch/get-view +test-db+ "views" "pages"))]
              (are [n page]
                   (= (dissoc (nth results n) :_id :_rev)
                      (let [uri (str "http://www.deeply-nested-dummy.com/"
                                     page)
                            filename (str
                                      "resources/test-data/deeply-nested/"
                                      page)]
                        {:type "crawled-page"
                         :crawl-tag "test-weighted-crawl"
                         :crawl-timestamp "2012-05-13T21:52:58.114Z"
                         :status 200
                         :score (float 0.1)
                         :trace-redirects [uri]
                         :crawled-at "2012-05-13T21:52:58.114Z"
                         :uri uri
                         :headers {}
                         :body (slurp filename)}))
                   0 "index.html"
                   1 "nested-0a.html"
                   2 "nested-0b.html"
                   3 "nested-1a.html"
                   4 "nested-1b.html"
                   5 "nested-2a.html"
                   6 "nested-2b.html"
                   7 "nested-3a.html"
                   8 "nested-3b.html"
                   9 "nested-4a.html"
                   10 "nested-4b.html"
                   11 "nested-5a.html"
                   12 "nested-5b.html"
                   13 "nested-6a.html"
                   14 "nested-6b.html"
                   15 "nested-7a.html"
                   16 "nested-7b.html"
                   17 "nested-8a.html"
                   18 "nested-8b.html"
                   19 "nested-9a.html"
                   20 "nested-9b.html"))))))

    (testing "test partly restricting page storage with page-scoring-fn"
      (with-test-db
        (with-redefs [util/make-timestamp #(str "2012-05-13T21:52:58.114Z")]
          (db/create-views +test-db+)
          (let [page-scoring-fn
                (fn [uri request-data]
                  (if (= (first
                          (:content
                           (first (enlive/select
                                   (enlive/html-resource
                                    (StringReader. (:body request-data)))
                                   [:title]))))
                         "Nested 0")
                    0 ;; don't store if the title is "Nested 0"
                    (float 0.1)))]
            @(weighted-crawl +test-db+
                             "test-weighted-crawl"
                             0
                             "http://www.deeply-nested-dummy.com/index.html"
                             page-scoring-fn)

            (let [results (map :value
                               (clutch/get-view +test-db+ "views" "pages"))]
              (is (= (count results) 19))
              (are [n page]
                   (= (dissoc (nth results n) :_id :_rev)
                      (let [uri (str "http://www.deeply-nested-dummy.com/"
                                     page)
                            filename (str
                                      "resources/test-data/deeply-nested/"
                                      page)]
                        {:type "crawled-page"
                         :crawl-tag "test-weighted-crawl"
                         :crawl-timestamp "2012-05-13T21:52:58.114Z"
                         :status 200
                         :score (float 0.1)
                         :trace-redirects [uri]
                         :crawled-at "2012-05-13T21:52:58.114Z"
                         :uri uri
                         :headers {}
                         :body (slurp filename)}))
                   0 "index.html"
                   1 "nested-1a.html"
                   2 "nested-1b.html"
                   3 "nested-2a.html"
                   4 "nested-2b.html"
                   5 "nested-3a.html"
                   6 "nested-3b.html"
                   7 "nested-4a.html"
                   8 "nested-4b.html"
                   9 "nested-5a.html"
                   10 "nested-5b.html"
                   11 "nested-6a.html"
                   12 "nested-6b.html"
                   13 "nested-7a.html"
                   14 "nested-7b.html"
                   15 "nested-8a.html"
                   16 "nested-8b.html"
                   17 "nested-9a.html"
                   18 "nested-9b.html"))))))

    (testing "test partly restricting crawling with page-scoring-fn"
      (with-test-db
        (with-redefs [util/make-timestamp #(str "2012-05-13T21:52:58.114Z")]
          (db/create-views +test-db+)
          (let [page-scoring-fn
                (fn [uri request-data]
                  (if (some
                       #{(first
                          (:content
                           (first (enlive/select
                                   (enlive/html-resource
                                    (StringReader. (:body request-data)))
                                   [:title]))))}
                       ["Nested"
                        "Nested 0"
                        "Nested 1"
                        "Nested 2"
                        "Nested 3"])
                    (float 0.1)
                    -0.1))]
               
            @(weighted-crawl +test-db+
                             "test-weighted-crawl"
                             0
                             "http://www.deeply-nested-dummy.com/index.html"
                             page-scoring-fn)

            (let [results (map :value
                               (clutch/get-view +test-db+ "views" "pages"))]
              (is (= (count results) 9))
              (are [n page]
                   (= (dissoc (nth results n) :_id :_rev)
                      (let [uri (str "http://www.deeply-nested-dummy.com/"
                                     page)
                            filename (str
                                      "resources/test-data/deeply-nested/"
                                      page)]
                        {:type "crawled-page"
                         :crawl-tag "test-weighted-crawl"
                         :crawl-timestamp "2012-05-13T21:52:58.114Z"
                         :status 200
                         :score (float 0.1)
                         :trace-redirects [uri]
                         :crawled-at "2012-05-13T21:52:58.114Z"
                         :uri uri
                         :headers {}
                         :body (slurp filename)}))
                   0 "index.html"
                   1 "nested-0a.html"
                   2 "nested-0b.html"
                   3 "nested-1a.html"
                   4 "nested-1b.html"
                   5 "nested-2a.html"
                   6 "nested-2b.html"
                   7 "nested-3a.html"
                   8 "nested-3b.html"))))))

    (testing "test partly restricting crawling with link-checker-fn"
      (with-test-db
        (with-redefs [util/make-timestamp #(str "2012-05-13T21:52:58.114Z")]
          (db/create-views +test-db+)
          (let [page-scoring-fn
                (fn [uri request-data]
                  (float 0.1))
                link-checker-fn
                (fn [uri]
                  (if (some
                       #{uri}
                       ["http://www.deeply-nested-dummy.com/index.html"
                        "http://www.deeply-nested-dummy.com/nested-0a.html"
                        "http://www.deeply-nested-dummy.com/nested-0b.html"
                        "http://www.deeply-nested-dummy.com/nested-1a.html"
                        "http://www.deeply-nested-dummy.com/nested-1b.html"
                        "http://www.deeply-nested-dummy.com/nested-2a.html"
                        "http://www.deeply-nested-dummy.com/nested-2b.html"
                        "http://www.deeply-nested-dummy.com/nested-3a.html"
                        "http://www.deeply-nested-dummy.com/nested-3b.html"])
                    true
                    false))]
               
            @(weighted-crawl +test-db+
                             "test-weighted-crawl"
                             0
                             "http://www.deeply-nested-dummy.com/index.html"
                             page-scoring-fn
                             link-checker-fn)

            (let [results (map :value
                               (clutch/get-view +test-db+ "views" "pages"))]
              (is (= (count results) 9))
              (are [n page]
                   (= (dissoc (nth results n) :_id :_rev)
                      (let [uri (str "http://www.deeply-nested-dummy.com/"
                                     page)
                            filename (str
                                      "resources/test-data/deeply-nested/"
                                      page)]
                        {:type "crawled-page"
                         :crawl-tag "test-weighted-crawl"
                         :crawl-timestamp "2012-05-13T21:52:58.114Z"
                         :status 200
                         :score (float 0.1)
                         :trace-redirects [uri]
                         :crawled-at "2012-05-13T21:52:58.114Z"
                         :uri uri
                         :headers {}
                         :body (slurp filename)}))
                   0 "index.html"
                   1 "nested-0a.html"
                   2 "nested-0b.html"
                   3 "nested-1a.html"
                   4 "nested-1b.html"
                   5 "nested-2a.html"
                   6 "nested-2b.html"
                   7 "nested-3a.html"
                   8 "nested-3b.html"))))))

    (testing "make sure that pages aren't recrawled within the hour"
      (with-test-db
        (db/create-views +test-db+)
        (let [page-scoring-fn
              (fn [uri request-data]
                (float 0.1))]
               
          @(weighted-crawl +test-db+
                           "test-weighted-crawl"
                           0
                           "http://www.deeply-nested-dummy.com/index.html"
                           page-scoring-fn)

          @(weighted-crawl +test-db+
                           "test-weighted-crawl"
                           0
                           "http://www.deeply-nested-dummy.com/index.html"
                           page-scoring-fn)
            
          (is (= (count (clutch/get-view +test-db+ "views" "pages")) 21)))))

    (testing "make sure that sleep time is respected"
      (with-test-db
        (db/create-views +test-db+)
        (let [page-scoring-fn
              (fn [uri request-data]
                (float 0.1))]
               
          @(weighted-crawl +test-db+
                           "test-weighted-crawl"
                           100
                           "http://www.deeply-nested-dummy.com/index.html"
                           page-scoring-fn)

          (let [crawled-times
                (map #(time-coerce/to-long
                       (time-format/parse
                        (time-format/formatters :date-time)
                        (:crawled-at (:value %))))
                     (sort-by #(:crawled-at (:value %))
                              (clutch/get-view +test-db+ "views" "pages")))]
            (is (= (count crawled-times) 21))

            (are [v]
                 (> (- (nth crawled-times (second v))
                       (nth crawled-times (first v)))
                    100)
                 [0 1]
                 [1 2]
                 [2 3]
                 [3 4]
                 [4 5]
                 [5 6]
                 [6 7]
                 [7 8]
                 [8 9]
                 [9 10]
                 [10 11]
                 [11 12]
                 [12 13]
                 [13 14]
                 [14 15]
                 [15 16]
                 [17 18]
                 [18 19]
                 [19 20])))))

    (testing "test if external links are handled"
      (with-test-db
        (db/create-views +test-db+)
        (let [page-scoring-fn
              (fn [uri request-data]
                (float 0.1))]
               
          @(weighted-crawl +test-db+
                           "test-weighted-crawl"
                           0
                           "http://www.dummyhealthfoodstore.com/index.html"
                           page-scoring-fn)

          ;; give the second thread some time to catch up
          (Thread/sleep 200)

          (is
           (=
            (map #(:uri (:value %))
                 (clutch/get-view +test-db+ "views" "pages"))
            ["http://www.dummyhealthfoodstore.com/ardbeg.html"
             "http://www.dummyhealthfoodstore.com/blackwoods-flake.html"
             "http://www.dummyhealthfoodstore.com/brora.html"
             "http://www.dummyhealthfoodstore.com/clynelish.html"
             "http://www.dummyhealthfoodstore.com/glp-meridian.html"
             "http://www.dummyhealthfoodstore.com/glp-westminster.html"
             "http://www.dummyhealthfoodstore.com/index.html"
             "http://www.dummyhealthfoodstore.com/london-mixture.html"
             "http://www.dummyhealthfoodstore.com/macallan.html"
             "http://www.dummyhealthfoodstore.com/navy-rolls.html"
             "http://www.dummyhealthfoodstore.com/port-ellen.html"
             "http://www.dummyhealthfoodstore.com/products/pipe-tobacco.html"
             "http://www.dummyhealthfoodstore.com/products/whisky.html"
             "http://www.vixu.com/"])))))))