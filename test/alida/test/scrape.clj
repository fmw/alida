;; test/alida/test/scrape.clj: tests for scraping functions
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

(ns alida.test.scrape
  (:use [clojure.test]
        [clj-http.fake]
        [alida.test.helpers :only [with-test-db +test-db+ dummy-routes]]
        [alida.scrape] :reload)
  (:require [net.cgrand.enlive-html :as enlive]
            [alida.crawl :as crawl]
            [alida.util :as util]
            [alida.db :as db]
            [clojure.pprint])
  (:import [java.io StringReader]))

(deftest test-get-links-enlive
  (let [html (slurp "resources/test-data/dummy-shop/whisky.html")]
    (is (= (get-links-enlive "http://www.dummyhealthfoodstore.com/index.html"
                             html
                             [:a])
           #{"http://www.dummyhealthfoodstore.com/clynelish.html"
             "http://www.dummyhealthfoodstore.com/en/About.html"
             "http://www.dummyhealthfoodstore.com/products/pipe-tobacco.html"
             "http://www.dummyhealthfoodstore.com/ardbeg.html"
             "http://www.dummyhealthfoodstore.com/brora.html"
             "http://www.dummyhealthfoodstore.com/nl"
             "http://www.dummyhealthfoodstore.com/"
             "http://www.dummyhealthfoodstore.com/en/Contact.html"
             "http://www.dummyhealthfoodstore.com/products/whisky.html"
             "http://www.dummyhealthfoodstore.com/port-ellen.html"
             "http://www.dummyhealthfoodstore.com/macallan.html"}))))

(deftest test-get-links-jsoup
  (let [html (slurp "resources/test-data/dummy-shop/whisky.html")]
    (is (= (get-links-jsoup "http://www.dummyhealthfoodstore.com/index.html"
                            html)
           #{"http://www.dummyhealthfoodstore.com/clynelish.html"
             "http://www.dummyhealthfoodstore.com/en/About.html"
             "http://www.dummyhealthfoodstore.com/products/pipe-tobacco.html"
             "http://www.dummyhealthfoodstore.com/ardbeg.html"
             "http://www.dummyhealthfoodstore.com/brora.html"
             "http://www.dummyhealthfoodstore.com/nl"
             "http://www.dummyhealthfoodstore.com/"
             "http://www.dummyhealthfoodstore.com/en/Contact.html"
             "http://www.dummyhealthfoodstore.com/products/whisky.html"
             "http://www.dummyhealthfoodstore.com/port-ellen.html"
             "http://www.dummyhealthfoodstore.com/macallan.html"}))))

(deftest test-get-links
  (let [html (slurp "resources/test-data/dummy-shop/whisky.html")
        index-html (slurp "resources/test-data/dummy-shop/index.html")]
    (is (= (get-links "http://www.dummyhealthfoodstore.com/" html [:a])
           (get-links "http://www.dummyhealthfoodstore.com/" html [:a] nil)
           #{"http://www.dummyhealthfoodstore.com/clynelish.html"
             "http://www.dummyhealthfoodstore.com/en/About.html"
             "http://www.dummyhealthfoodstore.com/products/pipe-tobacco.html"
             "http://www.dummyhealthfoodstore.com/ardbeg.html"
             "http://www.dummyhealthfoodstore.com/brora.html"
             "http://www.dummyhealthfoodstore.com/nl"
             "http://www.dummyhealthfoodstore.com/"
             "http://www.dummyhealthfoodstore.com/en/Contact.html"
             "http://www.dummyhealthfoodstore.com/products/whisky.html"
             "http://www.dummyhealthfoodstore.com/port-ellen.html"
             "http://www.dummyhealthfoodstore.com/macallan.html"}))

    (is (= (get-links "http://www.dummyhealthfoodstore.com/"
                      html
                      [:a]
                      {:path-filter #"^/products.+"})
           ["http://www.dummyhealthfoodstore.com/products/pipe-tobacco.html"
            "http://www.dummyhealthfoodstore.com/products/whisky.html"]))

    (is (= (get-links "http://www.dummyhealthfoodstore.com/"
                      index-html
                      [:a])
           #{"http://www.vixu.com/"
             "http://www.dummyhealthfoodstore.com/en/Contact.html"
             "http://www.dummyhealthfoodstore.com/products/pipe-tobacco.html"
             "http://www.dummyhealthfoodstore.com/nl"
             "http://www.dummyhealthfoodstore.com/"
             "http://www.dummyhealthfoodstore.com/products/whisky.html"
             "http://www.dummyhealthfoodstore.com/en/About.html"}))

    (is (= (get-links "http://www.dummyhealthfoodstore.com/"
                      index-html
                      [:a]
                      {:filter #"http://www.vixu.com.*"})
           (get-links "http://www.dummyhealthfoodstore.com/"
                      index-html
                      [:a]
                      {:filter #"http://www.vixu.com.*"
                       :path-filter #"/"})
           ["http://www.vixu.com/"]))

    (is (= (get-links "http://www.dummyhealthfoodstore.com/"
                      index-html
                      [:a]
                      {:path-filter #"/"})
           ["http://www.vixu.com/"
            "http://www.dummyhealthfoodstore.com/"]))))

(deftest test-html-to-plaintext
  (is (= (html-to-plaintext
          (slurp "resources/test-data/dummy-shop/index.html"))
         (str "Dummy Shop Dummy Shop Search Nederlands "
              "Home Whisky Pipe Tobacco About Contact "
              "Welcome to the Dummy Health Food Store! "
              "This health food dummy store only sells whisky "
              "and pipe tobacco! "
              "Powered by Vixu.com "
              "© 2012, Dummy Store Incorporated. All rights reserved."))))

(deftest test-get-trimmed-content
  (let [html (slurp "resources/test-data/dummy-shop/glp-westminster.html")
        resource (enlive/html-resource (StringReader. html))]
    (is (= (get-trimmed-content html [:#description])
           (get-trimmed-content resource [:#description])
           "Flavorful English mixture with a healthy dose of Latakia."))

    (is (= (get-trimmed-content html [[:#content]
                                      [:div]
                                      [:span (enlive/nth-of-type 2)]])
           (get-trimmed-content resource [[:#content]
                                          [:div]
                                          [:span (enlive/nth-of-type 2)]])
           ["Heirloom Collection: Westminster"
            "G. L. Pease"
            "Gregory L. Pease"
            "Virginia, Latakia and Oriental"
            "Ribbon"]))))

(deftest test-full-scrape
  (with-test-db
    (with-redefs [util/make-timestamp #(str "2012-05-19T22:08:56.250Z")]
      (do
        (db/create-views +test-db+)
        (with-fake-routes dummy-routes
          (db/add-batched-documents
           +test-db+
           @(crawl/directed-crawl
             "full-scrape-test"
             0
             "http://www.dummyhealthfoodstore.com/index.html"
             [{:selector [:ul#menu :a]
               :path-filter #"^/products.+"
               :next [{:selector [[:div#content] [:a]]}]}]))))

      (is (nil? (full-scrape +test-db+
                             "full-scrape-test"
                             "2012-05-19T22:08:56.250Z"
                             (fn [raw-page]
                               {:page-title
                                (first
                                 (:content
                                  (first (enlive/select
                                          (enlive/html-resource
                                           (StringReader. (:body raw-page)))
                                          [:title]))))
                                :fulltext
                                (html-to-plaintext (:body raw-page))})
                             3)))

      (is (= (map #(dissoc % :fulltext :_id :_rev)
                  (:documents
                   (db/get-scrape-results +test-db+
                                          "full-scrape-test"
                                          "2012-05-19T22:08:56.250Z"
                                          1000)))
             [{:crawled-at "2012-05-19T22:08:56.250Z",
                :uri (str "http://www.dummyhealthfoodstore.com/"
                          "products/whisky.html"),
                :type "scrape-result",
                :crawl-tag "full-scrape-test",
                :crawl-timestamp "2012-05-19T22:08:56.250Z",
                :page-title "Dummy Shop: whisky overview"}
               {:crawled-at "2012-05-19T22:08:56.250Z",
                :uri (str "http://www.dummyhealthfoodstore.com/"
                          "products/pipe-tobacco.html"),
                :type "scrape-result",
                :crawl-tag "full-scrape-test",
                :crawl-timestamp "2012-05-19T22:08:56.250Z",
                :page-title "Dummy Shop: pipe tobacco overview"}
               {:crawled-at "2012-05-19T22:08:56.250Z",
                :uri "http://www.dummyhealthfoodstore.com/port-ellen.html",
                :type "scrape-result",
                :crawl-tag "full-scrape-test",
                :crawl-timestamp "2012-05-19T22:08:56.250Z",
                :page-title "Dummy Shop: Port Ellen 30yo"}
               {:crawled-at "2012-05-19T22:08:56.250Z",
                :uri "http://www.dummyhealthfoodstore.com/navy-rolls.html",
                :type "scrape-result",
                :crawl-tag "full-scrape-test",
                :crawl-timestamp "2012-05-19T22:08:56.250Z",
                :page-title "Dummy Shop: Dunhill De Luxe Navy Rolls"}
               {:crawled-at "2012-05-19T22:08:56.250Z",
                :uri "http://www.dummyhealthfoodstore.com/macallan.html",
                :type "scrape-result",
                :crawl-tag "full-scrape-test",
                :crawl-timestamp "2012-05-19T22:08:56.250Z",
                :page-title "Dummy Shop: Macallan 30yo"}
               {:crawled-at "2012-05-19T22:08:56.250Z",
                :uri (str "http://www.dummyhealthfoodstore.com"
                          "/london-mixture.html"),
                :type "scrape-result",
                :crawl-tag "full-scrape-test",
                :crawl-timestamp "2012-05-19T22:08:56.250Z",
                :page-title "Dummy Shop: Dunhill London Mixture"}
               {:crawled-at "2012-05-19T22:08:56.250Z",
                :uri "http://www.dummyhealthfoodstore.com/index.html",
                :type "scrape-result",
                :crawl-tag "full-scrape-test",
                :crawl-timestamp "2012-05-19T22:08:56.250Z",
                :page-title "Dummy Shop"}
               {:crawled-at "2012-05-19T22:08:56.250Z",
                :uri (str "http://www.dummyhealthfoodstore.com"
                          "/glp-westminster.html"),
                :type "scrape-result",
                :crawl-tag "full-scrape-test",
                :crawl-timestamp "2012-05-19T22:08:56.250Z",
                :page-title "Dummy Shop: GLP Westminster"}
               {:crawled-at "2012-05-19T22:08:56.250Z",
                :uri "http://www.dummyhealthfoodstore.com/glp-meridian.html",
                :type "scrape-result",
                :crawl-tag "full-scrape-test",
                :crawl-timestamp "2012-05-19T22:08:56.250Z",
                :page-title "Dummy Shop: GLP Meridian"}
               {:crawled-at "2012-05-19T22:08:56.250Z",
                :uri "http://www.dummyhealthfoodstore.com/clynelish.html",
                :type "scrape-result",
                :crawl-tag "full-scrape-test",
                :crawl-timestamp "2012-05-19T22:08:56.250Z",
                :page-title "Dummy Shop: Clynelish 30yo"}
               {:crawled-at "2012-05-19T22:08:56.250Z",
                :uri "http://www.dummyhealthfoodstore.com/brora.html",
                :type "scrape-result",
                :crawl-tag "full-scrape-test",
                :crawl-timestamp "2012-05-19T22:08:56.250Z",
                :page-title "Dummy Shop: Brora 30yo"}
               {:crawled-at "2012-05-19T22:08:56.250Z",
                :uri (str "http://www.dummyhealthfoodstore.com"
                          "/blackwoods-flake.html"),
                :type "scrape-result",
                :crawl-tag "full-scrape-test",
                :crawl-timestamp "2012-05-19T22:08:56.250Z",
                :page-title "Dummy Shop: McClelland Blackwoods Flake"}
               {:crawled-at "2012-05-19T22:08:56.250Z",
                :uri "http://www.dummyhealthfoodstore.com/ardbeg.html",
                :type "scrape-result",
                :crawl-tag "full-scrape-test",
                :crawl-timestamp "2012-05-19T22:08:56.250Z",
                :page-title "Dummy Shop: Ardbeg 10yo"}])))))