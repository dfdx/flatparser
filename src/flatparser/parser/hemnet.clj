(ns flatparser.parser.hemnet
  (:require [clj-http.client :as client])
  (:import [org.eclipse.jetty.util UrlEncoded MultiMap]
           [java.io StringReader])
  (:use [net.cgrand enlive-html]
        [clojure.java io])
  (:gen-class))

(defn extract-base-url
  "Extracts Hemnet base URL from fake full URL with search parameters
  (fake because it makes no sense to invoke the URL directly as POST request must be issued before results can be viewed)"
  [url-p1]
  (first (.split url-p1 "\\?")))

(defn extract-search-params
  "Extracts query string from fake full URL and converts it to the map"
  [url-p1]
  (let [param-string (last (.split url-p1 "\\?")), param-map (MultiMap.)]
    (UrlEncoded/decodeTo param-string param-map "UTF-8")
    (into {} (map (fn [[k v]] [(keyword k) v]) param-map))))

(defn make-resource
  [url cookie-store]
  (let [response (get (client/get url {:cookie-store cookie-store}) :body) page-number (if (.contains url "=") (last (.split url "=")) 1)]
  (with-open [wr (make-writer (str "hemnet-results-" page-number ".html") {:encoding "UTF-8"})]
    (.write wr response))
  (html-resource (StringReader. response))))

(defn collect-from
  "TODO: Запросить данные один раз, сложить результат в файл и написать отдельную функцию парсинга файла из ФС, чтобы не дёргать каждый раз Hemnet"
  [url cookie-store info]
  (println "Collecting data from:" url)
  (let [parsed-resource (make-resource url cookie-store)]
    (println (get (get (first (select parsed-resource [[:meta (attr= :name "description")]])) :attrs) :content))))

(defn fetch-results
  "Sends POST request to prepare server-side object with information about search parameters and parses first N pages of results
  TODO: тут бы хорошо таки указать сортировку по другим параметрам. По умолчанию сортируется как попало: то по цене, то по времени нахождения на сайте"
  [base-url search-params num-pages info]
  (let [post-url (str base-url "/sok/create")]
    (println "Sending POST with params" search-params "to:" post-url)
    (let [my-cs (clj-http.cookies/cookie-store)]
      (client/post post-url {:debug false,
                             :debug-body false,
                             :cookie-store my-cs,
                             :form-params {:commit "",
                                          :search
                                          (into {} (map
                                                     (fn [[k v]]
                                                       (if (or (= k :municipality_ids) (= k :item_types))
                                                         [k {"" v}]
                                                         [k v]))
                                                     search-params))}})
      (let [results-url (str base-url "/resultat"),
            list-pages (cons results-url (map #(str results-url "?page=" %) (range 2 (+ num-pages 1))))]
        (apply concat (map #(collect-from % my-cs info) list-pages))))))

(defn collect-data
  "Collects data from Hemnet site"
  [url-p1 num-pages info]
  (let [base-url (extract-base-url url-p1) search-params (extract-search-params url-p1)]
    (fetch-results base-url search-params num-pages info))
  )