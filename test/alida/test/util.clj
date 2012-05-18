;; test/alida/test/util.clj: tests for utility functions
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

(ns alida.test.util
  (:use [clojure.test]
        [alida.util] :reload)
  (:require [clj-time.core :as time-core]))

(deftest test-make-timestamp
  (is (re-matches
        #"^[\d]{4}-[\d]{2}-[\d]{2}T[\d]{2}:[\d]{2}:[\d]{2}\.[\d]{1,4}Z"
        (make-timestamp))))

(deftest test-rfc3339-to-jodatime
  (let [datetime-obj (rfc3339-to-jodatime "2011-11-04T09:16:52.253Z"
                                          "Europe/Amsterdam")]
    (is (= (time-core/month datetime-obj) 11))
    (is (= (time-core/day datetime-obj) 4))
    (is (= (time-core/year datetime-obj) 2011))
    (is (= (time-core/hour datetime-obj) 10))
    (is (= (time-core/minute datetime-obj) 16))
    (is (= (time-core/sec datetime-obj) 52))
    (is (= (time-core/milli datetime-obj) 253)))

  (is (= (rfc3339-to-jodatime nil "GMT") nil)))

(deftest test-get-absolute-uri
  (are [base-uri link expected-uri]
       (= (get-absolute-uri base-uri link) expected-uri)
       "http://www.dummyhealthfoodstore.com/index.html"
       "/products/whisky.html"
       "http://www.dummyhealthfoodstore.com/products/whisky.html"
       "http://www.dummyhealthfoodstore.com/products/whisky.html"
       "../brora.html"
       "http://www.dummyhealthfoodstore.com/brora.html"))

(deftest test-get-uri-segments
  (is (= (get-uri-segments "http://www.dummyhealthfoodstore.com/brora.html")
         {:scheme "http://"
          :host "www.dummyhealthfoodstore.com"
          :path "/brora.html"})))