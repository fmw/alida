;; test/alida/test/helpers.clj: helper functions for unit tests
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
(ns alida.test.helpers
  (:require [com.ashafa.clutch :as clutch]))

(defn random-lower-case-string [length]
  ;; to include uppercase
  ;; (let [ascii-codes (concat (range 48 58) (range 66 91) (range 97 123))])
  (let [ascii-codes (concat (range 48 58) (range 97 123))]
    (apply str (repeatedly length #(char (rand-nth ascii-codes))))))

(def +test-db+ (str "alida-test-" (random-lower-case-string 20)))
(def +test-server+ "http://localhost:5984/")

(defmacro with-test-db
  [& body]
  `(try
     (clutch/get-database +test-db+)
     ~@body
     (finally
      (clutch/delete-database +test-db+))))

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