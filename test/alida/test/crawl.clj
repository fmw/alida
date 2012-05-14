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
  (:require [clj-http.client :as http-client]
            [clj-time.format :as time-format]
            [clj-time.core :as time-core]
            [clj-time.coerce :as time-coerce])
  (:use [clojure.test]
        [clj-http.fake]
        [alida.crawl] :reload))

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
                dummy-deeply-nested-uris))))

(deftest test-make-timestamp
  (is (re-matches
        #"^[\d]{4}-[\d]{2}-[\d]{2}T[\d]{2}:[\d]{2}:[\d]{2}\.[\d]{1,4}Z"
        (make-timestamp))))

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
    (with-redefs [make-timestamp #(str "2012-05-13T21:52:58.114Z")]
      (is
       (=
        (sort-by
         :uri
         @(directed-crawl 0
                          "http://www.dummyhealthfoodstore.com/index.html"
                          [{:selector [:ul#menu :a]
                            :path-filter #"^/products.+"
                            :next [{:selector [[:div#content] [:a]]}]}]))
        (sort-by
         :uri
         @(directed-crawl 0
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
                (zipmap [:uri
                         :crawled-at
                         :trace-redirects
                         :status
                         :headers
                         :body]
                        [uri
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