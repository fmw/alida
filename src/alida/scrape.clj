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
  (:require [clojure.string :as string]
            [net.cgrand.enlive-html :as enlive]
            [alida.util :as util]
            [alida.db :as db])
  (:import [java.io StringReader]
           [org.jsoup Jsoup]))

(defn get-links-enlive
  "Returns the set of unique href attribute values from
  the elements that match the provided enlive selector
  (e.g. [:a]) in the given string using the current-uri
  value to turn the links into absolute URIs."
  [current-uri s selector]
  (into #{}
        (map #(util/get-absolute-uri current-uri (:href (:attrs %)))
             (enlive/select (enlive/html-resource (StringReader. s))
                            selector))))

(defn get-links
  "Returns the set of unique href attribute values from the elements
  that match the provided enlive selector (e.g. [:a]) in the given
  string using the provided current-uri to turn the links into absolute
  URIs. Optionally also accepts a map with filters as a third argument,
  which supports :filter (ran over the absolute URI, including
  the hostname and scheme) and :path-filter (only ran over the
  path segment of the URI). "
  [current-uri
   s
   selector
   & [filters]]
  (let [links (get-links-enlive current-uri s selector)]
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
                                  (:path (util/get-uri-segments %))))
                    filtered-links)
                   filtered-links)
                 (rest filter-pairs))
          filtered-links)))))

(defn get-links-jsoup
  "Returns a set of absolute URIs for all links in a given html string
   using the provided current-uri as a base. This fn provides less
   control than the get-links-enlive fn, but offers much better
   performance."
  [current-uri html]
  (into #{}
        (map (fn [element]
               (util/get-absolute-uri current-uri (.attr element "href")))
             (.select (Jsoup/parse html) "a[href]"))))

(defn #^String html-to-plaintext
  "Returns the plaintext value of the provided HTML string.

   This function only returns the actual text of the page. This
   doesn't include attribute values like image titles. See the
   vix/lucene/distill-plaintext fn for an example of a function that
   also includes img attributes."
  [html]
  (.text (Jsoup/parse html)))

(defmulti get-trimmed-content
  "Returns a string or sequence of strings (if multiple values are
   found) given either a string containing html or the output of
   Enlive's html-resource fn as the first argument and a selector
   vector as the second argument."
  (fn [page selector]
    (cond
     (string? page) :string
     (seq? page) :enlive-resource)))

(defmethod get-trimmed-content :enlive-resource [resource selector]
  (let [selection (enlive/select resource selector)]
    (cond
     (= (count selection) 1)
     (string/trim (apply str (:content (first selection))))
     (> (count selection) 1)
     (map (fn [node]
            (string/trim (apply str (:content node))))
          selection))))

(defmethod get-trimmed-content :string [html selector]
  (get-trimmed-content (enlive/html-resource (StringReader. html)) selector))

(defn full-scrape
  "Processes crawl results with the provided crawl-tag and
   crawl-timestamp that are stored in the database by running
   scraping-fn over them. The scraping-fn should always return a map
   or nil. This fn requests and stores documents in quantities of
   batch-size (optional)."
  [database crawl-tag crawl-timestamp scraping-fn & [batch-size]]
  (let [limit (or batch-size 1000)]
    (loop [raw (db/get-pages-for-crawl-tag-and-timestamp database
                                                         crawl-tag
                                                         crawl-timestamp
                                                         limit)]
      (db/add-batched-documents database
                                (map (fn [raw-page]
                                       (assoc (scraping-fn raw-page)
                                         :type "scrape-result"
                                         :uri (:uri raw-page)
                                         :crawled-at (:crawled-at raw-page)
                                         :crawl-tag crawl-tag
                                         :crawl-timestamp crawl-timestamp))
                                     (:documents raw)))
      (when-let [{:keys [uri timestamp id]} (:next raw)]
        (recur (db/get-pages-for-crawl-tag-and-timestamp database
                                                         crawl-tag
                                                         crawl-timestamp
                                                         limit
                                                         uri
                                                         timestamp
                                                         id))))))