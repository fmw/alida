;; test/alida/test/lucene.clj: tests for lucene functions
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

(ns alida.test.lucene
  (:use [clojure.test]
        [alida.lucene] :reload)
  (:require [alida.util :as util])
  (:import [org.apache.lucene.document FieldType$NumericType]
           [org.apache.lucene.search QueryWrapperFilter]
           [org.apache.lucene.index
            IndexWriter IndexWriterConfig$OpenMode]
           [org.apache.lucene.search BooleanClause$Occur ScoreDoc]))

(deftest test-create-field-type
  (let [field-type (create-field-type :int :stored :indexed :tokenized)]
    (is (.tokenized field-type))
    (is (.stored field-type))
    (is (.indexed field-type))
    (is (= (.numericType field-type) FieldType$NumericType/INT)))

  (let [field-type (create-field-type :int :indexed :tokenized)]
    (is (.tokenized field-type))
    (is (not (.stored field-type)))
    (is (.indexed field-type))
    (is (= (.numericType field-type) FieldType$NumericType/INT)))

  (let [field-type (create-field-type :int :tokenized)]
    (is (.tokenized field-type))
    (is (not (.stored field-type)))
    (is (not (.indexed field-type)))
    (is (= (.numericType field-type) FieldType$NumericType/INT)))
  
  (let [field-type (create-field-type :int)]
    (is (not (.tokenized field-type)))
    (is (not (.stored field-type)))
    (is (not (.indexed field-type)))
    (is (= (.numericType field-type) FieldType$NumericType/INT)))

  (let [field-type (create-field-type :float)]
    (is (not (.tokenized field-type)))
    (is (not (.stored field-type)))
    (is (not (.indexed field-type)))
    (is (= (.numericType field-type) FieldType$NumericType/FLOAT)))

  (let [field-type (create-field-type :double)]
    (is (not (.tokenized field-type)))
    (is (not (.stored field-type)))
    (is (not (.indexed field-type)))
    (is (= (.numericType field-type) FieldType$NumericType/DOUBLE)))

  (let [field-type (create-field-type :long)]
    (is (not (.tokenized field-type)))
    (is (not (.stored field-type)))
    (is (not (.indexed field-type)))
    (is (= (.numericType field-type) FieldType$NumericType/LONG)))

  (let [field-type (create-field-type :string)]
    (is (not (.tokenized field-type)))
    (is (not (.stored field-type)))
    (is (not (.indexed field-type)))
    (is (= (.numericType field-type) nil)))

  (let [field-type (create-field-type :string :stored :indexed :tokenized)]
    (is (.tokenized field-type))
    (is (.stored field-type))
    (is (.indexed field-type))
    (is (= (.numericType field-type) nil))))

(deftest test-create-field
  (let [field (create-field "string-field"
                            "Hello, world!"
                            :tokenized
                            :indexed)]
    (is (= (.name field) "string-field"))
    (is (= (.stringValue field) "Hello, world!"))
    (is (.tokenized (.fieldType field)))
    (is (.indexed (.fieldType field))))

  (is (.stored (.fieldType (create-field "foo"  "bar" :stored))))

  (let [field (create-field "long-field"
                            1
                            :tokenized
                            :indexed
                            :stored)]
    (is (= (.name field) "long-field"))
    (is (= (.numericValue field) 1))
    (is (.tokenized (.fieldType field)))
    (is (.indexed (.fieldType field)))
    (is (.stored (.fieldType field))))

  (let [field (create-field "double-field"
                            1.0
                            :indexed
                            :stored)]
    (is (= (.name field) "double-field"))
    (is (= (.numericValue field) 1.0))
    (is (not (.tokenized (.fieldType field))))
    (is (.indexed (.fieldType field)))
    (is (.stored (.fieldType field))))

  (let [field (create-field "float-field"
                            (float 1.0)
                            :indexed)]
    (is (= (.name field) "float-field"))
    (is (= (.numericValue field) (float 1.0)))
    (is (not (.tokenized (.fieldType field))))
    (is (.indexed (.fieldType field)))
    (is (not (.stored (.fieldType field)))))

  (let [field (create-field "int-field"
                            (int 1)
                            :stored)]
    (is (= (.name field) "int-field"))
    (is (= (.numericValue field) (int 1.0)))
    (is (not (.tokenized (.fieldType field))))
    (is (not (.indexed (.fieldType field))))
    (is (.stored (.fieldType field)))))

(deftest test-set-field-value!
  (is (= (.stringValue
          (set-field-value! (create-field "test" "foo" :stored) "bar"))
         "bar"))

  (is (= (.numericValue
          (set-field-value! (create-field "test" 1 :stored) 42))
         42))

  (is (= (.numericValue
          (set-field-value! (create-field "test" 1.0 :stored) 2.0))
         2.0))

  (is (= (.numericValue
          (set-field-value! (create-field "test" (float 1.0) :stored)
                            (float 2.0)))
         (float 2.0)))

  (is (= (.numericValue
          (set-field-value! (create-field "test" (int 3) :stored) (int 42)))
         (int 42))))

(deftest test-create-document-
  (let [[amount-field fulltext-field title-field]
        (.getFields
         (create-document-
          (sorted-map :title (create-field "title"
                                           ""
                                           :stored
                                           :indexed
                                           :tokenized)
                      :fulltext (create-field "fulltext"
                                              ""
                                              :indexed
                                              :tokenized)
                      :amount (create-field "amount"
                                            0
                                            :indexed
                                            :stored))
          {:title "Hello, world!"
           :fulltext "Hello, world! Hic sunt dracones."
           :amount 1}))]
    (is (= (.stringValue title-field) "Hello, world!"))
    (is (= (.stringValue fulltext-field) "Hello, world! Hic sunt dracones."))
    (is (= (.numericValue amount-field) 1))))

(deftest test-create-analyzer
    (testing "test if Lucene analyzers are created correctly."
      (is (= (class (create-analyzer))
             org.apache.lucene.analysis.standard.StandardAnalyzer))))

(deftest test-create-directory
    (testing "test if Lucene directories are created correctly."
      (is (= (class (create-directory :RAM))
             org.apache.lucene.store.RAMDirectory))
      (let [directory (create-directory "/tmp/test")]
        (is (= (class directory) org.apache.lucene.store.NIOFSDirectory))
        (is (or 
              (= (str (.getDirectory directory)) "/tmp/test")
              (= (str (.getDirectory directory)) "/private/tmp/test"))))))

(deftest test-create-index-reader
  (testing "test if Lucene IndexReaders are created correctly."
    (let [dir (create-directory :RAM)]
      ;; before an index is written to the directory, expect nil
      (is (= (create-index-reader dir) nil))
      ;; write to index to get an actual IndexReader
      (with-open  [writer (create-index-writer (create-analyzer)
                                               dir
                                               :create)]
        (add-documents-to-index! writer
                                 {:title (create-field "title"
                                                       ""
                                                       :stored
                                                       :indexed
                                                       :tokenized)}
                                 [{:title "1"}]))
      (is (= (class (create-index-reader dir)) 
             org.apache.lucene.index.StandardDirectoryReader)))))

(deftest test-create-index-writer
  (testing "test if index writers are created correctly"
    (let [directory (create-directory :RAM)
          analyzer (create-analyzer)]
      
      (with-open [writer (create-index-writer analyzer directory :create)]
        (is (= (class writer) IndexWriter))
        (is (= (.getAnalyzer writer) analyzer))
        (is (= (.getDirectory writer) directory))
          
        (let [config (.getConfig writer)]
          (is (= (.getRAMBufferSizeMB config) 49.0))
          (is (= (.getOpenMode config) IndexWriterConfig$OpenMode/CREATE))))

      
      (with-open [writer (create-index-writer analyzer directory :append)]
        (is (= (.getOpenMode (.getConfig writer))
               IndexWriterConfig$OpenMode/APPEND)))
      
      (with-open [writer (create-index-writer analyzer
                                              directory
                                              :create-or-append)]
        (is (= (.getOpenMode (.getConfig writer))
               IndexWriterConfig$OpenMode/CREATE_OR_APPEND))))))

(deftest test-add-documents-to-index!
  (let [analyzer (create-analyzer)
        dir (create-directory :RAM)
        fields-map {:title (create-field "title"
                                         ""
                                         :stored
                                         :indexed
                                         :tokenized)}]
    (with-open [writer (create-index-writer analyzer dir :create)]
      (add-documents-to-index! writer
                               fields-map
                               [{:title "1"}
                                {:title "2"}
                                {:title "3"}
                                {:title "4"}
                                {:title "5"}
                                {:title "6"}
                                {:title "7"}
                                {:title "8"}
                                {:title "9"}
                                {:title "10"}
                                {:title "11"}]))

    (let [reader (create-index-reader dir)]
      (is (= (map #(.get % "title")
                  (get-docs reader (map #(ScoreDoc. % 1.0) (range 11))))
             (map str (range 1 12)))))))

(deftest test-create-date-field
  (let [date-field (create-date-field "published"
                                 "2012-05-21T04:21:33.248Z"
                                 :indexed
                                 :stored)]
    (is (= (.name date-field) "published"))
    (is (= (.numericValue date-field) 1337574093248))
    (is (not (.tokenized (.fieldType date-field))))
    (is (.indexed (.fieldType date-field)))
    (is (.stored (.fieldType date-field)))))

(deftest test-create-term-query
  (let [tq (create-term-query "foo" "bar")
        term (.getTerm tq)]
    (is (= (.field term) "foo"))
    (is (= (.text term) "bar"))))

(deftest test-create-numeric-range-query
  (let [long-query (create-numeric-range-query "long-query" 3 42)]
    (is (= (class long-query)
           org.apache.lucene.search.NumericRangeQuery))

    (is (= (.getMin long-query) 3))
    (is (= (.getMax long-query) 42)))

  (let [int-query (create-numeric-range-query "int-query" (int 3) (int 42))]
    (is (= (class int-query)
           org.apache.lucene.search.NumericRangeQuery))

    (is (= (.getMin int-query) (int 3)))
    (is (= (.getMax int-query) (int 42))))

  (let [float-query (create-numeric-range-query "float-query"
                                                (float 3.01)
                                                (float 42.01))]
    (is (= (class float-query)
           org.apache.lucene.search.NumericRangeQuery))

    (is (= (.getMin float-query) (float 3.01)))
    (is (= (.getMax float-query) (float 42.01))))

  (let [double-query (create-numeric-range-query "double-query" 3.03 42.03)]
    (is (= (class double-query)
           org.apache.lucene.search.NumericRangeQuery))

    (is (= (.getMin double-query) 3.03))
    (is (= (.getMax double-query) 42.03)))

  (is (= (create-numeric-range-query "bad-query" 42 3) nil))
  (is (= (create-numeric-range-query "bad-query" 42 nil) nil))
  (is (= (create-numeric-range-query "bad-query" 42.03 3.03) nil))
  (is (= (create-numeric-range-query "bad-query" 42.03 nil) nil))
  (is (= (create-numeric-range-query "bad-query" (int 42) (int 3)) nil))
  (is (= (create-numeric-range-query "bad-query" (int 42) nil) nil))
  (is (= (create-numeric-range-query "bad-query"
                                     (float 42.5)
                                     (float 3.5))
         nil))
  (is (= (create-numeric-range-query "bad-query" (float 42.5) nil) nil)))

(deftest test-create-date-range-query
  (let [query (create-date-range-query "42"
                                       "1985-08-04T09:00:00.0Z"
                                       "2012-01-15T17:54:45.0Z")]
    (is (= (class query)
           org.apache.lucene.search.NumericRangeQuery))

    (is (= (.getMin query)
           (util/rfc3339-to-long "1985-08-04T09:00:00.0Z")))
    (is (= (.getMax query)
           (util/rfc3339-to-long "2012-01-15T17:54:45.0Z")))))

(deftest test-create-boolean-query
  (is (= (create-boolean-query) nil))
  (is (= (create-boolean-query (create-term-query "foo" "bar")) nil))

  (let [foo-query (create-term-query "foo" "bar")
        a-query (create-term-query "a" "b")
        x-query (create-term-query "x" "y")
        bq (create-boolean-query foo-query :must
                                 a-query :must-shot
                                 x-query :should)
        [foo-clause a-clause x-clause] (.getClauses bq)]
    (is (= (class bq) org.apache.lucene.search.BooleanQuery))

    (is (= (.getQuery foo-clause) foo-query))
    (is (= (.getOccur foo-clause) BooleanClause$Occur/MUST))
    
    (is (= (.getQuery a-clause) a-query))
    (is (= (.getOccur a-clause) nil))
    
    (is (= (.getQuery x-clause) x-query))
    (is (= (.getOccur x-clause) BooleanClause$Occur/SHOULD))))

(deftest test-get-doc
  (let [analyzer (create-analyzer)
        dir (create-directory :RAM)
        fields-map {:title (create-field "title"
                                         ""
                                         :stored
                                         :indexed
                                         :tokenized)}]
    (with-open [writer (create-index-writer analyzer dir :create)]
      (add-documents-to-index! writer fields-map [{:title "bar"}]))

    (let [reader (create-index-reader dir)]
      (is (= (.get (get-doc reader 0) "title") "bar")))))

(deftest test-get-docs
  (let [analyzer (create-analyzer)
        dir (create-directory :RAM)
        fields-map {:title (create-field "title"
                                         ""
                                         :stored
                                         :indexed
                                         :tokenized)}]
    (with-open [writer (create-index-writer analyzer dir :create)]
      (add-documents-to-index! writer
                               fields-map
                               [{:title "Hic sunt dracones"}
                                {:title "Brora!"}
                                {:title "Caol Ila"}]))

    (let [reader (create-index-reader dir)]
      (is (= (map #(.get % "title")
                  (get-docs reader [(ScoreDoc. 0 1.0)
                                    (ScoreDoc. 1 1.0)
                                    (ScoreDoc. 2 1.0)]))
             ["Hic sunt dracones" "Brora!" "Caol Ila"])))))