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
    (is (= (get-links-enlive html [:a])
           #{"clynelish.html"
             "/en/About.html"
             "/products/pipe-tobacco.html"
             "ardbeg.html"
             "brora.html"
             "/nl"
             "/"
             "/en/Contact.html"
             "/products/whisky.html"
             "port-ellen.html"
             "macallan.html"}))))

(deftest test-get-links
  (let [html (slurp "resources/test-data/dummy-shop/whisky.html")]
    (is (= (get-links html [:a])
           (get-links html [:a] nil)
           #{"clynelish.html"
             "/en/About.html"
             "/products/pipe-tobacco.html"
             "ardbeg.html"
             "brora.html"
             "/nl"
             "/"
             "/en/Contact.html"
             "/products/whisky.html"
             "port-ellen.html"
             "macallan.html"}))

    (is (= (get-links html [:a] #"^/products.+")
           ["/products/pipe-tobacco.html"
            "/products/whisky.html"]))))