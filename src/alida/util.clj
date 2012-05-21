;; src/alida/util.clj: utility functions
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

(ns alida.util
  (:require [clj-time.format :as time-format]
            [clj-time.core :as time-core]
            [clj-time.coerce :as time-coerce])
  (:import [java.net URL]))

(defn make-timestamp
  "Returns a string with the RFC 3339 timestamp for the current date/time.
   e.g. 2012-05-13T21:52:58.114Z"
  []
  (time-format/unparse
   (time-format/formatters :date-time)
   (time-core/now)))

(defn rfc3339-to-jodatime [date-string timezone]
  "Converts a RFC3339 formatted date string into an org.joda.time.DateTime
   object for the provided timezone."
  (when (and (string? date-string) (not (= date-string "")))
    (time-core/to-time-zone
     (time-format/parse
      (time-format/formatters :date-time) date-string)
     (time-core/time-zone-for-id timezone))))

(defn rfc3339-to-long
  [date-string]
  "Converts RFC3339 formatted date string to microseconds since UNIX epoch.
   Throws NullPointerException on incorrect input."
  (when (and (string? date-string) (not (= date-string "")))
    (time-coerce/to-long (time-format/parse date-string))))

(defn get-absolute-uri
  "Returns the absolute URI of a link using the provided current-uri."
  [current-uri link]
  (str (URL. (URL. current-uri) link)))

(defn get-uri-segments [uri]
  "Returns a map with the :scheme, :host and :path for the given URI."
  (zipmap [:scheme :host :path]
          (rest (first (re-seq #"^([^/]+\/{2})([^/]+)(\/{1}.*)" uri)))))