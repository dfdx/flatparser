(ns flatparser.parser.hemnet
  (:require [clj-http.client :as client])
  (:import [org.eclipse.jetty.util UrlEncoded MultiMap]
           [java.io StringReader])
  (:use [net.cgrand enlive-html]
        [clojure.java io]
        [flatparser geo util]
        [flatparser.parser putil])
  (:gen-class ))

(def subway-stations-green-line
  ["Hässelby strand"
   "Hässelby gård"
   "Johannelund"
   "Vällingby"
   "Råksta"
   "Blackberg"
   "Islandstorget"
   "Ängbyplan"
   "Åkeshov"
   "Brommaplan"
   "Abrahamsberg"
   "Stora Mossen"
   "Alvik"
   "Kristineberg"
   "Thorildsplan"
   "Fridhemsplan"
   "S:t Eriksplan"
   "Odenplan"
   "Rådmansgatan"
   "Hötorget"
   "T-Centralen"
   "Gamla stan"
   "Slussen"
   "Medborgarplatsen"
   "Skanstull"
   "Gullmarsplan"
   "Globen"
   "Enskede gård"
   "Sockenplan"
   "Svedmyra"
   "Stureby"
   "Bandhagen"
   "Högdalen"
   "Rågsved"
   "Hagsätra"
   "Skärmarbrink"
   "Blåsut"
   "Sandsborg"
   "Skogskyrkogården"
   "Tallkrogen"
   "Gubbängen"
   "Hökarängen"
   "Farsta"
   "Farsta strand"
   "Hammarbyhöjden"
   "Björkhagen"
   "Kärrtorp"
   "Bagarmossen"
   "Skarpnäck"])

(def subway-coords-green-line
  [
    [59.36128660000001 17.8323518] [59.3669003 17.8437692] [59.3679426 17.8574655] [59.36324949999999 17.8720631] [59.3548066 17.8818198]
    [59.3483529 17.8827972] [59.34585689999999 17.8940196] [59.34188520000001 17.9070478] [59.3420363 17.9248998] [59.33838979999999 17.9392617]
    [59.33668340000001 17.9529522] [59.334534 17.9661914] [59.3336355 17.9802646] [59.33281110000001 18.003182] [59.3318137 18.0154371]
    [59.3343715 18.0324055] [59.3396541 18.0369877] [59.3429559 18.0497038] [59.3405724 18.0587755] [59.33553310000001 18.063539]
    [59.3309466 18.0592629] [59.3231599 18.0676203] [59.3194951 18.072331] [59.31433730000001 18.073551] [59.30785210000001 18.0762277]
    [59.2991161 18.0807724] [59.2942798 18.0779742] [59.2893938 18.0702939] [59.28330020000001 18.0705911] [59.27763840000001 18.0672321]
    [59.27459769999999 18.0556262] [59.2704022 18.0494902] [59.2637969 18.0430047] [59.2565804 18.0281349] [59.26272840000001 18.0124874]
    [59.2953638 18.0904409] [59.2902433 18.0910631] [59.2847872 18.0923821] [59.27919180000001 18.0954989] [59.2711356 18.0853267]
    [59.2628777 18.0820388] [59.2579216 18.0824974] [59.243553 18.0932839] [59.23500769999999 18.1017369] [59.29476529999999 18.1045506]
    [59.29111390000001 18.1155212] [59.2845051 18.1144767] [59.27626429999999 18.131465] [59.2668195 18.133347]
    ])

(def param-names
  {
    :price "Pris"
    :price_square_meter "Pris/m²"
    :monthly_fee "Avgift/månad"
    :room_no "Antal rum"
    :size_t "Boarea"
    :year_built "Byggår"
    })

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

(defn make-resource-from-url
  [url cookie-store]
  (let [response (:body (client/get url {:cookie-store cookie-store}))]
    (html-resource (StringReader. response))))

(defn fetch-street-address [resource]
  (let [street-view-components (select resource [:div#item-streetview ])]
    (cond (empty? street-view-components) NA
      :else (.trim (:data-address (:attrs (first street-view-components)))))))

(defn full-address [street-and-house]
  (str street-and-house ", Sverige"))

(defn param [params name]
  (params (param-names name)))

(defn parse-price [price-str]
  (cond (nil? price-str) NA
    :else (parse-int (.replaceAll price-str "[^0-9]*" " "))))

(defn parse-year [year-str]
  (cond (nil? year-str) NA
    (.contains year-str "-") (parse-int (.substring year-str 0 (.indexOf year-str "-")))
    (.contains year-str "/") (parse-int (.substring year-str 0 (.indexOf year-str "/")))
    :else (parse-int year-str)))

(defn parse-dist-to-subway [coords]
  (if-not (empty? coords)
    (nearest-subway subway-coords-green-line coords)
    NA))

(defn parse-dist-to-key-point [coords key-point]
  (if key-point
    (distance coords (coords-by-addr (full-address key-point)))
    NA))

(defn fetch-params [url cookie-store info]
  (let [resource (make-resource-from-url url cookie-store)
        facts (select resource [:div#item-fact-comparison :tbody ])
        names (map #(.trim (first (:content %))) (select facts [:tr :> first-child]))
        values (map #(.trim (first (:content %))) (select facts [:tr :td.current ]))
        params (zipmap names values)
        address (fetch-street-address resource)
        coords (coords-by-addr (full-address address))]
    {
      :price (parse-price (param params :price ))
      :price_square_meter (parse-price (param params :price_square_meter ))
      :monthly_fee (parse-price (param params :monthly_fee ))
      :address address
      :lat (first coords)
      :lon (second coords)
      :dist_to_subway (parse-dist-to-subway coords)
      :dist_to_kp (parse-dist-to-key-point coords (:kp info))
      :room_no (parse-int (param params :room_no ))
      :size_t (parse-double (param params :size_t ))
      :size_l NA
      :size_k NA
      :state NA
      :floor NA
      :floors NA
      :restroom NA
      :balcony NA
      :year_built (parse-year (param params :year_built ))
      :walls NA
      :furniture NA
      :url url
      }))

(defn fetch-apartment-urls [search-results-url cookie-store]
  (let [parsed-resource (make-resource-from-url search-results-url cookie-store)]
    (map (comp #(str "http://www.hemnet.se" %) :href :attrs ) (select parsed-resource [:ul#search-results :div.item :div.image :a.item-link ]))))

(defn fetch-results
  "Sends POST request to prepare server-side object with information about search parameters and parses first N pages of results"
  [base-url search-params num-pages info]
  (let [post-url (str base-url "/sok/create")]
    (let [my-cs (clj-http.cookies/cookie-store)]
      (client/post post-url {:debug false,
                             :debug-body false,
                             :cookie-store my-cs,
                             :form-params {:commit "",
                                           :search (into {} (map
                                                              (fn [[k v]]
                                                                (if (or (= k :municipality_ids ) (= k :item_types ))
                                                                  [k {"" v}]
                                                                  [k v]))
                                                              search-params))}})
      (let [results-url (str base-url "/resultat"),
            sort-url (str results-url "/sortera?by=price"),
            sort-order-url (str results-url "/sortera?order=desc"),
            list-pages (cons results-url (map #(str results-url "?page=" %) (range 2 (+ num-pages 1))))]
        (client/get sort-url {:cookie-store my-cs})
        (client/get sort-order-url {:cookie-store my-cs})
        (let [ap-urls (apply concat (map #(fetch-apartment-urls % my-cs) list-pages))]
          (map #(do
                  (println "Fetching parameters from:" %)
                  (fetch-params % my-cs info)) ap-urls))))))

(defn collect-data
  "Collects data from Hemnet site"
  [url-p1 num-pages info]
  (let [base-url (extract-base-url url-p1) search-params (extract-search-params url-p1)]
    (fetch-results base-url search-params num-pages info))
  )