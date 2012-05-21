;; src/alida/lucene.clj: Apache Lucene search-related functions
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

(ns alida.lucene
  (:import [org.apache.lucene.document
            Document
            Field
            FieldType
            FieldType$NumericType
            DoubleField
            FloatField
            IntField
            LongField
            StoredField
            StringField
            TextField]))


(defn #^FieldType create-field-type [data-type & options]
  "Creates a Lucene FieldType object given a data-type
   (either :string, :double, :float, :int or :long) and options
   (:indexed, :tokenized, :store).

   tokenized:
   tokenizes the value (breaks it up) for fulltext search.

   stored:
   makes the field retrievable from the index, instead of just
   processable at search time.

   :indexed:
   makes the field searchable and filterable.

   Nota bene: a field needs to be either :stored or :indexed.
   Also, it doesn't make sense to tokenize numeric values."
  (let [field-type (FieldType.)]
    ;; note - if only used for sorting (and not querying/filtering)
    ;; setting precisionStep to Integer.MAX_VALUE is more efficient
    ;; for numeric fields (this will minimize disk space consumed).
    (cond
     (= data-type :double)
     (.setNumericType field-type FieldType$NumericType/DOUBLE)
     (= data-type :float)
     (.setNumericType field-type FieldType$NumericType/FLOAT)
     (= data-type :int)
     (.setNumericType field-type FieldType$NumericType/INT)
     (= data-type :long)
     (.setNumericType field-type FieldType$NumericType/LONG))
    
    (doto field-type
      (.setIndexed (not (nil? (some #{:indexed} options))))
      (.setStored (not (nil? (some #{:stored} options))))
      (.setTokenized (not (nil? (some #{:tokenized} options))))
      (.freeze))))

(defmulti #^Field create-field
  "Creates a field with the given name and value that has a
   FieldType created using the provided options."
  (fn [name value & options]
    (class value)))

(defmethod create-field java.lang.String [name value & options]
  (Field. name
          (or value "")
          (apply (partial create-field-type :string) options)))

(defmethod create-field java.lang.Long [name value & options]
  (LongField. name
              (or value 0)
              (apply (partial create-field-type :long) options)))

(defmethod create-field java.lang.Integer [name value & options]
  (IntField. name
                 (or value (int 0))
                 (apply (partial create-field-type :int)  options)))

(defmethod create-field java.lang.Float [name value & options]
  (FloatField. name
               (or value (float 0.0))
               (apply (partial create-field-type :float) options)))

(defmethod create-field java.lang.Double [name value & options]
  (DoubleField. name
                (or value 0.0)
                (apply (partial create-field-type :double) options)))

(defmulti #^Field set-field-value
  "Sets the value of the Lucene Field object to the provided value.
   The Lucene documentation recommends this approach over creating
   new fields for every document for performance reasons."
  (fn [field value]
    (class value)))

(defmethod set-field-value java.lang.String [field value]
  (doto field
    (.setStringValue value)))

(defmethod set-field-value java.lang.Long [field value]
  (doto field
    (.setLongValue value)))

(defmethod set-field-value java.lang.Integer [field value]
  (doto field
    (.setIntValue value)))

(defmethod set-field-value java.lang.Float [field value]
  (doto field
    (.setFloatValue value)))

(defmethod set-field-value java.lang.Double [field value]
  (doto field
    (.setDoubleValue value)))

(defn #^Document create-document
  "Takes two hash maps, fields-map with a Lucene Field instance
   for every key, and values-map with the same keys mapped to
   the desired values for the document. Creates a Lucene Document
   object with the Field instances from the fields-map set to the
   values provided in the value-map."
  [fields-map values-map]
  (let [doc (Document.)]
    (doseq [[k field] fields-map]
      (.add doc (set-field-value field (get values-map k))))
    doc))