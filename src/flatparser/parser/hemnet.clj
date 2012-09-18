(ns flatparser.parser.hemnet
  (:require [clj-http.client :as client])
  (:import [org.eclipse.jetty.util UrlEncoded MultiMap]
           [java.io StringReader])
  (:use [net.cgrand enlive-html]
        [clojure.java io]
        [flatparser geo util]
        [flatparser.parser putil])
  (:gen-class ))

(def subway-stations
  {"(G) Hässelby strand" [59.36128660000001 17.8323518]
   "(G) Hässelby gård" [59.3669003 17.8437692]
   "(G) Johannelund" [59.3679426 17.8574655]
   "(G) Vällingby" [59.36324949999999 17.8720631]
   "(G) Råksta" [59.3548066 17.8818198]
   "(G) Blackberg" [59.3483529 17.8827972]
   "(G) Islandstorget" [59.34585689999999 17.8940196]
   "(G) Ängbyplan" [59.34188520000001 17.9070478]
   "(G) Åkeshov" [59.3420363 17.9248998]
   "(G) Brommaplan" [59.33838979999999 17.9392617]
   "(G) Abrahamsberg" [59.33668340000001 17.9529522]
   "(G) Stora Mossen" [59.334534 17.9661914]
   "(G) Alvik" [59.3336355 17.9802646]
   "(G) Kristineberg" [59.33281110000001 18.003182]
   "(G) Thorildsplan" [59.3318137 18.0154371]
   "(GB) Fridhemsplan" [59.3343715 18.0324055]
   "(G) S:t Eriksplan" [59.3396541 18.0369877]
   "(G) Odenplan" [59.3429559 18.0497038]
   "(G) Rådmansgatan" [59.3405724 18.0587755]
   "(G) Hötorget" [59.33553310000001 18.063539]
   "(RGB) T-Centralen" [59.3309466 18.0592629]
   "(RG) Gamla stan" [59.3231599 18.0676203]
   "(RG) Slussen" [59.3194951 18.072331]
   "(G) Medborgarplatsen" [59.31433730000001 18.073551]
   "(G) Skanstull" [59.30785210000001 18.0762277]
   "(G) Gullmarsplan" [59.2991161 18.0807724]
   "(G) Globen" [59.2942798 18.0779742]
   "(G) Enskede gård" [59.2893938 18.0702939]
   "(G) Sockenplan" [59.28330020000001 18.0705911]
   "(G) Svedmyra" [59.27763840000001 18.0672321]
   "(G) Stureby" [59.27459769999999 18.0556262]
   "(G) Bandhagen" [59.2704022 18.0494902]
   "(G) Högdalen" [59.2637969 18.0430047]
   "(G) Rågsved" [59.2565804 18.0281349]
   "(G) Hagsätra" [59.26272840000001 18.0124874]
   "(G) Skärmarbrink" [59.2953638 18.0904409]
   "(G) Blåsut" [59.2902433 18.0910631]
   "(G) Sandsborg" [59.2847872 18.0923821]
   "(G) Skogskyrkogården" [59.27919180000001 18.0954989]
   "(G) Tallkrogen" [59.2711356 18.0853267]
   "(G) Gubbängen" [59.2628777 18.0820388]
   "(G) Hökarängen" [59.2579216 18.0824974]
   "(G) Farsta" [59.243553 18.0932839]
   "(G) Farsta strand" [59.23500769999999 18.1017369]
   "(G) Hammarbyhöjden" [59.29476529999999 18.1045506]
   "(G) Björkhagen" [59.29111390000001 18.1155212]
   "(G) Kärrtorp" [59.2845051 18.1144767]
   "(G) Bagarmossen" [59.27626429999999 18.131465]
   "(G) Skarpnäck" [59.2668195 18.133347]
   "(R) Mörby centrum" [59.3987091 18.0362204]
   "(R) Danderyds sjukhus" [59.3919041 18.0413718]
   "(R) Bergshamra" [59.38150790 18.03651610]
   "(R) Universitetet" [59.3655675 18.0548911]
   "(R) Tekniska Högskolan" [59.348357 18.072032]
   "(R) Stadion" [59.3429669 18.0817007]
   "(R) Östermalmstorg" [59.3349729 18.0740827]
   "(R) Mariatorget" [59.3169577 18.0633084]
   "(R) Zinkensdamm" [59.3177777 18.0501522]
   "(R) Hornstull" [59.3158365 18.0340214]
   "(R) Liljeholmen" [59.3107063 18.0231301]
   "(R) Midsommarkransen" [59.301852 18.0120361]
   "(R) Telefonplan" [59.29832139999999 17.9972352]
   "(R) Hägerstensåsen" [59.2955713 17.9791519]
   "(R) Västertorp" [59.291379 17.9666675]
   "(R) Fruängen" [59.285929 17.9650058]
   "(R) Ropsten" [59.3572983 18.1022179]
   "(R) Gårdet" [59.3472023 18.0987932]
   "(R) Karlaplan" [59.3388106 18.0908621]
   "(R) Aspudden" [59.30644900000001 18.0014459]
   "(R) Örnsberg" [59.3055324 17.9891998]
   "(R) Axelsberg" [59.3044155 17.9754315]
   "(R) Mälarhöjden" [59.3009181 17.957282]
   "(R) Bredäng" [59.29483829999999 17.9338136]
   "(R) Sätra" [59.28498399999999 17.92137]
   "(R) Skärholmen" [59.27714400000001 17.9070051]
   "(R) Vårberg" [59.2759332 17.8901577]
   "(R) Vårby gård" [59.2646124 17.8843974]
   "(R) Masmo" [59.2496816 17.8803326]
   "(R) Fittja" [59.24746560000001 17.8609676]
   "(R) Alby" [59.2394955 17.8453281]
   "(R) Hallunda" [59.2432692 17.825607]
   "(R) Norsborg" [59.24379089999999 17.8145252]
   "(B) Hjulsta" [59.3967421 17.8884515]
   "(B) Tensta" [59.3944823 17.9011652]
   "(B) Rinkeby" [59.3881634 17.9287814]
   "(B) Rissne" [59.37584080000001 17.9399593]
   "(B) Duvbo" [59.3678923 17.9646175]
   "(B) Sundbybergs centrum" [59.36089699999999 17.9722113]
   "(B) Vreten" [59.35418669999999 17.9739819]
   "(B) Huvudsta" [59.3495465 17.9856954]
   "(B) Västra skogen" [59.3474793 18.0039891]
   "(B) Stadshagen" [59.3369631 18.0173183]
   "(B) Rådhuset" [59.33029990000001 18.0420682]
   "(B) Kungsträdgården" [59.33078399999999 18.0732938]
   "(B) Akalla" [59.41481289999999 17.9127974]
   "(B) Husby" [59.410259 17.9256372]
   "(B) Kista" [59.4028644 17.9424336]
   "(B) Hallonbergen" [59.3754487 17.9692153]
   "(B) Näckrosen" [59.3667379 17.9832797]
   "(B) Solna centrum" [59.3588602 17.9989737]})

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

(defn find-nearest-subway [coords]
  (if-not (empty? coords)
    (let [dist-to-name (into {} (map (fn [[n c]] [(distance coords c) n]) subway-stations))
          min-dist (apply min (keys dist-to-name))]
      [(get dist-to-name min-dist) min-dist])
    [NA NA]))

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
        coords (coords-by-addr (full-address address))
        nearest-subway (find-nearest-subway coords)]
    {
      :price (parse-price (param params :price ))
      :price_square_meter (parse-price (param params :price_square_meter ))
      :monthly_fee (parse-price (param params :monthly_fee ))
      :address address
      :lat (first coords)
      :lon (second coords)
      :subway (first nearest-subway)
      :dist_to_subway (second nearest-subway)
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


(defn is-valid
  "Takes a maps and checks it for the precense of required parameters"
  [m]
  (and
    (not= (:price m) NA)
    (not= (:monthly_fee m) NA)
    (not= (:dist_to_subway m) NA)
    (not= (:room_no m) NA)
    (not= (:size_t m) NA)
    (<= (:dist_to_subway m) 650)))

(defn collect-data
  "Collects data from Hemnet site"
  [url-p1 num-pages info]
  (let [base-url (extract-base-url url-p1) search-params (extract-search-params url-p1)]
    (filter is-valid (fetch-results base-url search-params num-pages info))))