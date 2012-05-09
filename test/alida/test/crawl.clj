(ns alida.test.crawl
  (:use [clojure.test]
        [clj-http.fake]
        [alida.crawl] :reload))

(defn get-filename-from-uri [uri]
  (str "resources/test-data/dummy-shop/"
       (last (clojure.string/split uri #"/"))))

(defn dummy-response [file]
  (fn [req]
    {:status 200
     :headers {}
     :body (slurp file)}))

(def dummy-uris
  ["http://www.dummyhealthfoodstore.com/index.html"
   "http://www.dummyhealthfoodstore.com/products/whisky.html"
   "http://www.dummyhealthfoodstore.com/products/pipe-tobacco.html"
   "http://www.dummyhealthfoodstore.com/brora.html"
   "http://www.dummyhealthfoodstore.com/port-ellen.html"
   "http://www.dummyhealthfoodstore.com/macallan.html"
   "http://www.dummyhealthfoodstore.com/clynelish.html"
   "http://www.dummyhealthfoodstore.com/ardbeg.html"
   "http://www.dummyhealthfoodstore.com/glp-westminster.html"
   "http://www.dummyhealthfoodstore.com/glp-meridian.html"
   "http://www.dummyhealthfoodstore.com/blackwoods-flake.html"
   "http://www.dummyhealthfoodstore.com/navy-rolls.html"
   "http://www.dummyhealthfoodstore.com/london-mixture.html"])

(def dummy-routes
  (zipmap dummy-uris
          (map #(dummy-response (get-filename-from-uri %)) dummy-uris)))

(deftest test-get-page
  (with-fake-routes dummy-routes
    (are [uri]
         (= (get-page uri)
            {:trace-redirects [uri]
             :status 200
             :headers {}
             :body (slurp (get-filename-from-uri uri))})
         
         "http://www.dummyhealthfoodstore.com/index.html"
         "http://www.dummyhealthfoodstore.com/products/whisky.html"
         "http://www.dummyhealthfoodstore.com/products/pipe-tobacco.html"
         "http://www.dummyhealthfoodstore.com/brora.html"
         "http://www.dummyhealthfoodstore.com/port-ellen.html"
         "http://www.dummyhealthfoodstore.com/clynelish.html"
         "http://www.dummyhealthfoodstore.com/macallan.html"
         "http://www.dummyhealthfoodstore.com/ardbeg.html"
         "http://www.dummyhealthfoodstore.com/glp-westminster.html"
         "http://www.dummyhealthfoodstore.com/glp-meridian.html"
         "http://www.dummyhealthfoodstore.com/blackwoods-flake.html"
         "http://www.dummyhealthfoodstore.com/navy-rolls.html"
         "http://www.dummyhealthfoodstore.com/london-mixture.html")))