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
  (:import [org.apache.lucene.document FieldType$NumericType]))

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

(deftest test-set-field-value
  (is (= (.stringValue
          (set-field-value (create-field "test" "foo" :stored) "bar"))
         "bar"))

  (is (= (.numericValue
          (set-field-value (create-field "test" 1 :stored) 42))
         42))

  (is (= (.numericValue
          (set-field-value (create-field "test" 1.0 :stored) 2.0))
         2.0))

  (is (= (.numericValue
          (set-field-value (create-field "test" (float 1.0) :stored)
                           (float 2.0)))
         (float 2.0)))

  (is (= (.numericValue
          (set-field-value (create-field "test" (int 3) :stored) (int 42)))
         (int 42))))

(deftest test-create-document
  (let [[amount-field fulltext-field title-field]
        (.getFields
         (create-document
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
    (is (= (.numericValue amount-field) 1))
    ))