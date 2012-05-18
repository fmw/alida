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