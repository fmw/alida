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
        [alida.scrape] :reload))

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