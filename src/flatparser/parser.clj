(ns flatparser.parser
  (:refer-clojure)
  (:use [net.cgrand enlive-html]
        [clojure pprint repl]
        [clojure.java io]))

;; (def ex-res
;;   (html-resource
;;    (java.io.StringReader.
;;     (slurp "http://irr.tut.by/realestate/sale-flats/incity/6740237/"))))


(defn convert-price [price-str]
  (Integer/parseInt (.replaceAll price-str "[^0-9]" "")))

(defn fetch-params [page-res]
  (let [param-res (select page-res [:table#mainParams])
        names (rest (map (comp first :content) (select param-res [:span])))
        values (map (comp first :content) (select param-res [:div]))
        price (first (:content (first (select page-res [:b#priceSelected]))))]
    (assoc (zipmap names values) "Цена" (convert-price price))))


(defn fetch-links [list-res]
  (map (comp #(str "http://irr.by" %) :href :attrs)
       (select list-res [:td.tdTxt :div.h3 :a])))

(defn parse-page
  "Parses page with apartment description from irr.by.
   Returns map of apartment parameters"
  [url]
  (assoc (fetch-params (html-resource (reader url)))
    :url url))

(defn parse-list-page
  "Given URL (as string) of search results from irr.by
   parses every result and produces list of maps,
   each representing apartment parameters"
  [url]
  (doall (map #(do (println "Parsing" %) (parse-page %))
              (fetch-links (html-resource (reader url))))))

(defn parse-list [list-1-url]
  (let [list-pages (cons list-1-url (map #(str list-1-url "/page" % "/")
                                         (range 2 6)))]
    (apply concat (map parse-list-page list-pages))))