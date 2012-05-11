;; src/alida/scrape.clj: scraping utility functions.
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

(ns alida.scrape
  (:require [net.cgrand.enlive-html :as enlive])
  (:import [java.io StringReader]
           [org.jsoup Jsoup]))

(defn get-links-enlive
  "Returns the set of unique href attribute values from
  the elements that match the provided enlive selector
  (e.g. [:a]) in the given string."
  [s selector]
  (into #{}
        (map #(:href (:attrs %))
             (enlive/select (enlive/html-resource (StringReader. s))
                            selector))))

(defn get-links
  "Returns the set of unique href attribute values from
  the elements that match the provided enlive selector
  (e.g. [:a]) in the given string. Optionally also accepts
  a third argument containing a regular expression pattern
  to filter the output."
  ([s selector]
     (get-links-enlive s selector))
  ([s selector pattern]
     (filter #(re-matches pattern %) (get-links-enlive s selector))))