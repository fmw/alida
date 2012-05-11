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
        [alida.crawl] :reload))

(defn get-filename-from-uri [uri]
  (str "resources/test-data/dummy-shop/"
       (last (clojure.string/split uri #"/"))))

(defn dummy-response [file]
  (fn [req]
    {:status 200
     :headers {}
     :body (slurp file)}))

(def dummy-uris
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

(def dummy-routes
  (zipmap dummy-uris
          (map #(dummy-response (get-filename-from-uri %)) dummy-uris)))

(deftest test-get-page
  (with-fake-routes dummy-routes
    (are [uri]
         (= (get-page uri)
            {:trace-redirects [uri]
             :status 200
             :headers {}
             :body (slurp (get-filename-from-uri uri))})
         
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
         "http://www.dummyhealthfoodstore.com/london-mixture.html")))