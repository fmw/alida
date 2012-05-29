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

(deftest test-document-to-map
  (is (= (document-to-map
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
                                             :stored)
                       :double (create-field "double"
                                             0.0
                                             :indexed
                                             :stored))
           {:title "Hello, world!"
            :fulltext "Hello, world! Hic sunt dracones."
            :amount 1
            :double 0.3}))
         {:title "Hello, world!"
          :fulltext "Hello, world! Hic sunt dracones."
          :amount 1
          :double 0.3})))

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

(deftest test-valid-range-of-type?
  (is (true? (valid-range-of-type? 1 2 java.lang.Long)))
  (is (true? (valid-range-of-type? 1.0 2.0 java.lang.Double)))
  (is (false? (valid-range-of-type? 2 1 java.lang.Long)))
  (is (false? (valid-range-of-type? 1 2 java.lang.Float))))

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

(deftest test-create-query-wrapper-filter
  (is (= (create-query-wrapper-filter nil) nil))
  (let [query (create-term-query "foo" "bar")
        qwf (create-query-wrapper-filter query)]
    (is (= (class qwf) org.apache.lucene.search.QueryWrapperFilter))
    (is (= (.getQuery qwf) query))))

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

(deftest test-search
  (is (= (search "whisky"
                 nil
                 "fulltext"
                 10
                 (create-index-reader (create-directory :RAM))
                 (create-analyzer))
         {:total-hits 0
          :docs nil}))
  
  (let [analyzer (create-analyzer)
        dir (create-directory :RAM)
        documents [{:name "Ardbeg 10"
                    :distillery "Ardbeg"
                    :bottler "Distillery Bottling"
                    :volume 70
                    :abv 46.0
                    :vintage -1
                    :price 9.9
                    :fulltext (str "Ardbeg 10yo\n"
                                   "Ardbeg whisky")}
                   {:name "Brora 30yo"
                    :distillery "Brora"
                    :bottler "Distillery Bottling"
                    :volume 70
                    :abv 54.30
                    :vintage 1972
                    :price 25.0
                    :fulltext (str "Brora 30yo\n"
                                   "Brora whisky")}
                   {:name "Clynelish 30yo"
                    :distillery "Clynelish"
                    :bottler "Distillery Bottling"
                    :volume 70
                    :abv 49.30
                    :vintage 1980
                    :price 15.0
                    :fulltext (str "Clynelish 30yo\n"
                                   "Clynelish whisky")}
                   {:name "Port Ellen 30yo"
                    :distillery "Port Ellen"
                    :bottler "Distillery Bottling"
                    :volume 70
                    :abv 56.40
                    :vintage 1979
                    :price 35.0
                    :fulltext (str "Port Ellen 30yo\n"
                                   "Port Ellen whisky")}
                   {:name "Macallan 30yo"
                    :distillery "Macallan"
                    :bottler "Distillery Bottling"
                    :volume 70
                    :abv 41.30
                    :vintage 1970
                    :price 10.0
                    :fulltext (str "Macallan 30yo\n"
                                   "Macallan whisky")}]
        fields-map {:name (create-field "name"
                                        ""
                                        :stored
                                        :indexed)
                    :distillery (create-field "distillery"
                                              ""
                                              :stored
                                              :indexed)
                    :bottler (create-field "bottler"
                                           ""
                                           :stored
                                           :indexed)
                    :volume (create-field "volume"
                                          0
                                          :stored
                                          :indexed)
                    :abv (create-field "abv"
                                       0.0
                                       :stored
                                       :indexed)
                    :vintage (create-field "vintage"
                                           0
                                           :stored
                                           :indexed)
                    :price (create-field "price"
                                         0.0
                                         :stored
                                         :indexed)
                    :fulltext (create-field "fulltext"
                                            ""
                                            :indexed
                                            :tokenized)}]
    (with-open [writer (create-index-writer analyzer dir :create)]
      (add-documents-to-index! writer
                               fields-map
                               documents))

    (let [reader (create-index-reader dir)]
      (testing "Invalid query shouldn't raise an exception."
        (is (= (search "\"'foo"
                       nil
                       "fulltext"
                       10
                       reader
                       analyzer)
               nil)))
      
      (is (= (search "whisky"
                     nil
                     "fulltext"
                     10
                     reader
                     analyzer)
             {:total-hits 5
              :docs
              [{:name "Ardbeg 10"
                :bottler "Distillery Bottling"
                :index {:score (float 0.40883923), :doc-id 0}
                :price 9.9
                :distillery "Ardbeg"
                :abv 46.0
                :vintage -1
                :volume 70}
               {:name "Brora 30yo"
                :bottler "Distillery Bottling"
                :index {:score (float 0.40883923), :doc-id 1}
                :price 25.0
                :distillery "Brora"
                :abv 54.3
                :vintage 1972
                :volume 70}
               {:name "Clynelish 30yo"
                :bottler "Distillery Bottling"
                :index {:score (float 0.40883923), :doc-id 2}
                :price 15.0
                :distillery "Clynelish"
                :abv 49.3
                :vintage 1980
                :volume 70}
               {:name "Macallan 30yo"
                :bottler "Distillery Bottling"
                :index {:score (float 0.40883923), :doc-id 4}
                :price 10.0
                :distillery "Macallan"
                :abv 41.3
                :vintage 1970
                :volume 70}
               {:name "Port Ellen 30yo"
                :bottler "Distillery Bottling"
                :index {:score (float 0.30662942), :doc-id 3}
                :price 35.0
                :distillery "Port Ellen"
                :abv 56.4
                :vintage 1979
                :volume 70}]}))

      (is (= (search "whisky"
                     (create-query-wrapper-filter
                      (create-numeric-range-query "price" 5.0 20.0))
                     "fulltext"
                     10
                     reader
                     analyzer)
             {:total-hits 3
              :docs
              [{:name "Ardbeg 10"
                :bottler "Distillery Bottling"
                :index {:score (float 0.40883923), :doc-id 0}
                :price 9.9
                :distillery "Ardbeg"
                :abv 46.0
                :vintage -1
                :volume 70}
               {:name "Clynelish 30yo"
                :bottler "Distillery Bottling"
                :index {:score (float 0.40883923), :doc-id 2}
                :price 15.0
                :distillery "Clynelish"
                :abv 49.3
                :vintage 1980
                :volume 70}
               {:name "Macallan 30yo"
                :bottler "Distillery Bottling"
                :index {:score (float 0.40883923), :doc-id 4}
                :price 10.0
                :distillery "Macallan"
                :abv 41.3
                :vintage 1970
                :volume 70}]}))))

  (let [dir (create-directory :RAM)
        analyzer (create-analyzer)]
    (with-open [writer (create-index-writer analyzer dir :create)]
      (add-documents-to-index!
       writer
       {:n (create-field "title"
                         ""
                         :stored
                         :indexed)
        :fulltext (create-field "fulltext"
                                ""
                                :indexed
                                :tokenized)}
       (map (fn [n]
              {:n (str n)
               :fulltext "test"})
            (range 30))))

    (let [reader (create-index-reader dir)]
      (is (= (search "test"
                     nil
                     "fulltext"
                     10
                     reader
                     analyzer)
             {:total-hits 30,
              :docs
              [{:title "0"
                :index {:score (float 0.9672102) :doc-id 0}}
               {:title "1"
                :index {:score (float 0.9672102) :doc-id 1}}
               {:title "2"
                :index {:score (float 0.9672102) :doc-id 2}}
               {:title "3"
                :index {:score (float 0.9672102) :doc-id 3}}
               {:title "4"
                :index {:score (float 0.9672102) :doc-id 4}}
               {:title "5"
                :index {:score (float 0.9672102) :doc-id 5}}
               {:title "6"
                :index {:score (float 0.9672102) :doc-id 6}}
               {:title "7"
                :index {:score (float 0.9672102) :doc-id 7}}
               {:title "8"
                :index {:score (float 0.9672102) :doc-id 8}}
               {:title "9"
                :index {:score (float 0.9672102) :doc-id 9}}]}))

      (is (= (search "test"
                     nil
                     "fulltext"
                     10
                     reader
                     analyzer
                     9
                     0.9672102)
             {:total-hits 30,
              :docs
              [{:title "10"
                :index {:score (float 0.9672102), :doc-id 10}}
               {:title "11"
                :index {:score (float 0.9672102), :doc-id 11}}
               {:title "12"
                :index {:score (float 0.9672102), :doc-id 12}}
               {:title "13"
                :index {:score (float 0.9672102), :doc-id 13}}
               {:title "14"
                :index {:score (float 0.9672102), :doc-id 14}}
               {:title "15"
                :index {:score (float 0.9672102), :doc-id 15}}
               {:title "16"
                :index {:score (float 0.9672102), :doc-id 16}}
               {:title "17"
                :index {:score (float 0.9672102), :doc-id 17}}
               {:title "18"
                :index {:score (float 0.9672102), :doc-id 18}}
               {:title "19"
                :index {:score (float 0.9672102), :doc-id 19}}]}))

      (is (= (search "test"
                     nil
                     "fulltext"
                     10
                     reader
                     analyzer
                     19
                     0.9672102)
             {:total-hits 30,
              :docs
              [{:title "20"
                :index {:score (float 0.9672102), :doc-id 20}}
               {:title "21"
                :index {:score (float 0.9672102), :doc-id 21}}
               {:title "22"
                :index {:score (float 0.9672102), :doc-id 22}}
               {:title "23"
                :index {:score (float 0.9672102), :doc-id 23}}
               {:title "24"
                :index {:score (float 0.9672102), :doc-id 24}}
               {:title "25"
                :index {:score (float 0.9672102), :doc-id 25}}
               {:title "26"
                :index {:score (float 0.9672102), :doc-id 26}}
               {:title "27"
                :index {:score (float 0.9672102), :doc-id 27}}
               {:title "28"
                :index {:score (float 0.9672102), :doc-id 28}}
               {:title "29"
                :index {:score (float 0.9672102), :doc-id 29}}]})))))