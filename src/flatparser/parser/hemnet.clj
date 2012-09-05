(ns flatparser.parser.hemnet
  (:require [clj-http.client :as client])
  (:import [org.eclipse.jetty.util UrlEncoded MultiMap])
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

(defn create-search-request
  "TODO"
  [base-url search-params]
  (let [post-url (str base-url "/sok/create")]
    (println "Sending POST with params" search-params "to:" post-url)
    ;(client/post post-url :content-type "text/plain" :body "" )
    ))

(defn collect-from
  "TODO"
  ;issue a GET request to the URL to parse the page and extract needed data
  [url info]
  (println url))

(defn fetch-results
  "Fetches first n pages of search results"
  [base-url n info search-request]
  (let [fetch-results-url (str base-url "/resultat"),
        list-pages (cons fetch-results-url (map #(str fetch-results-url "?page=" %) (range 2 (+ n 1))))]
    (apply concat (map #(collect-from % info) list-pages))))

(defn collect-data
  "Issues a POST request to initilize search and collects data from n first pages of search results"
  [url-p1 n info]
  (let [base-url (extract-base-url url-p1)]
    (fetch-results base-url n info (create-search-request base-url (extract-search-params url-p1))))
  )