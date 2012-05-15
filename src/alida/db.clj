(ns alida.db
  (:require [com.ashafa.clutch :as clutch]))

(defn create-views [database]
  "Adds a design document called 'views' with the required views to
   the CouchDB database."
  (clutch/with-db database
    (clutch/save-view
     "views"
     (clutch/view-server-fns
      :cljs
      {:pages {:map (fn [doc]
                      (if (= (aget doc "type") "crawled-page")
                        (js/emit (to-array
                                  [(aget doc "uri") (aget doc "crawled-at")])
                                 doc)))}}))))

(defn add-batched-documents [database documents]
  "Adds provided documents to database."
  (clutch/bulk-update database documents))