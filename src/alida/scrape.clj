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
           [org.jsoup Jsoup]
           [java.net URL]))

(defn get-absolute-uri
  "Returns the absolute URI of a link using the provided current-uri."
  [current-uri link]
  (str (URL. (URL. current-uri) link)))

(defn get-uri-segments [uri]
  "Returns a map with the :scheme, :host and :path for the given URI."
  (zipmap [:scheme :host :path]
          (rest (first (re-seq #"^([^/]+\/{2})([^/]+)(\/{1}.*)" uri)))))

(defn get-links-enlive
  "Returns the set of unique href attribute values from
  the elements that match the provided enlive selector
  (e.g. [:a]) in the given string using the base-uri
  value to turn the links into absolute URIs."
  [base-uri s selector]
  (into #{}
        (map #(get-absolute-uri base-uri (:href (:attrs %)))
             (enlive/select (enlive/html-resource (StringReader. s))
                            selector))))

(defn get-links
  "Returns the set of unique href attribute values from the elements
  that match the provided enlive selector (e.g. [:a]) in the given
  string using the provided base-uri to turn the links into absolute
  URIs. Optionally also accepts a map with filters as a third argument,
  which supports :filter (ran over the absolute URI, including
  the hostname and scheme) and :path-filter (only ran over the
  path segment of the URI). "
  [base-uri
   s
   selector
   & [filters]]
  (let [links (get-links-enlive base-uri s selector)]
    (if (empty? filters)
      links
      (loop [filtered-links links
             filter-pairs (vec filters)]
        (if-let [[filter-name filter-pattern] (first filter-pairs)]
          (recur (if (= (class filter-pattern) java.util.regex.Pattern)
                   (filter
                    (cond
                     (= filter-name :filter)
                     #(re-matches filter-pattern %)
                     (= filter-name :path-filter)
                     #(re-matches filter-pattern
                                  (:path (get-uri-segments %))))
                    filtered-links)
                   filtered-links)
                 (rest filter-pairs))
          filtered-links)))))