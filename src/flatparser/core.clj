
(ns flatparser.core
  (:refer-clojure)
  (:use [clojure pprint repl]
        [clojure.data json]
        [clojure.java io]
        [flatparser util]
        [seesaw core])
  (:require [flatparser.parser
             [rent-n-sale :as rns]
             [irrby :as irrby]
             [bn :as bn]
		     [hata :as hata]
             [hemnet :as hemnet]])
  (:gen-class))


(def feat-order
  [:price :price_square_meter :monthly_fee :kind :clazz :room_no :size_t :size_l :size_k :dist_to_subway :dist_to_kp :state :beds :furniture :district :subway :floor :floors :year_built :walls :date :address :lat :lon :url ])

(defn map-values [order feats]
  (vec (map #(or (feats %) NA) order)))

(defn make-dataset
  "Takes sequence of maps, representing data, and produces sequence
   where first element is vector of attribute names
   and all the others are vectors of corresponding features.
   This is mostly to give some order to attributes"
  [maps]
  (cons feat-order (map #(map-values feat-order %) maps)))


(defn escape-str
  "If obj is string, escape"
  [obj]
  (if (and (= (class obj) String) (not= obj NA))
    (str "\"" obj "\"")
    obj))

(defn write-csv [filepath dataset]
  (with-open [wr (writer filepath)]
    (cl-format wr "~{~a~^;~}~%" (map #(.replaceAll (name %) "-" "_")
                                  (first dataset)))
    (doseq [obs (rest dataset)]
      (cl-format wr "~{~a~^;~}~%" (map escape-str obs)))))


(defn make-collector [url-p1]
  (cond
   (.startsWith url-p1 "http://irr.by") irrby/collect-data
   (.startsWith url-p1 "http://irr.tut.by") irrby/collect-data
   (.startsWith url-p1 "http://rent-and-sale.ru") rns/collect-data
   (.startsWith url-p1 "http://www.bn.ru") bn/collect-data
   (.startsWith url-p1 "http://www.hata.by") hata/collect-data
   (.startsWith url-p1 "http://www.hemnet.se") hemnet/collect-data
   :else nil))

;; shell

(defn run-shell [url-p1 n kp out-file]
  (write-csv out-file
    (make-dataset ((make-collector url-p1) url-p1 (parse-int n)
                    {:kp kp}))))

;; GUI

(native!)

(defn btn-listener [url num-pages search-params out-file-path]
  (write-csv out-file-path (make-dataset ((make-collector url) url (parse-int num-pages) search-params)))
  (alert (str "Dataset written to " out-file-path)))

(defn labeled-field
  [label-text field]
  (vertical-panel :items [(label label-text) field]))

(defn default-panel
  []
  (let [url-text (text)
        n-text (text)
        kp-text (text)
        out-file-text (text)
        btn (button :text "Parse" :halign :center )]
    (listen btn :action (fn [e] (btn-listener (text url-text) (text n-text) {:kp (text kp-text)} (text out-file-text))))
    (grid-panel
      :items [(label "URL of search result page") url-text
              (label "Number of pages to fetch") n-text
              (label "Key point (address)") kp-text
              (label "Path to output file") out-file-text
              (hide! (label)) btn]
      :columns 2
      :vgap 5
      :hgap 5
      :border 5)))

(defn hemnet-panel
  []
  (let [price-min (text "1000000")
        price-max (text "1200000")
        monthly-payment-min (text "1000")
        monthly-payment-max (text "4000")
        rooms-min (text "2")
        rooms-max (text "2")
        living-area-min (text "40")
        living-area-max (text "80")
        dist-to-subway-max (text "650")
        n-text (text "15")
        kp-text (text "Holländargatan 13, Stockholm")
        out-file-text (text "data.csv")
        btn (button :text "Search and parse" :halign :center )]
    (listen btn :action (fn [e]
                          (btn-listener "http://www.hemnet.se"
                            (text n-text)
                            (into {} (filter (fn [[k v]] (not= v NA)) {:kp (text kp-text)
                                                              :advanced 1
                                                              :age "all"
                                                              :item_types ["bostadsratt"]
                                                              :country_id 0
                                                              :region_id 17744
                                                              :municipality_ids [-1]
                                                              :home_swapping 0
                                                              :new_construction 0
                                                              :price_min (parse-int (text price-min))
                                                              :price_max (parse-int (text price-max))
                                                              :fee_min (parse-int (text monthly-payment-min))
                                                              :fee_max (parse-int (text monthly-payment-max))
                                                              :rooms_min (parse-int (text rooms-min))
                                                              :rooms_max (parse-int (text rooms-max))
                                                              :living_area_min (parse-int (text living-area-min))
                                                              :living_area_max (parse-int (text living-area-max))
                                                              :dist_to_subway_max (parse-int (text dist-to-subway-max))}))
                            (text out-file-text))))
    (grid-panel
      :items [(label "Looking for apartments in Stockholm, Sweden (all municipalities)") (hide! (label))
              (labeled-field "Price (min)" price-min) (labeled-field "Price (max)" price-max)
              (labeled-field "Monthly payment (min)" monthly-payment-min) (labeled-field "Monthly payment (max)" monthly-payment-max)
              (labeled-field "Number of rooms (min)" rooms-min) (labeled-field "Number of rooms (max)" rooms-max)
              (labeled-field "Living area (min)" living-area-min) (labeled-field "Living area (max)" living-area-max)
              (labeled-field "Dist to subway (max)" dist-to-subway-max) (labeled-field "Key point (address)" kp-text)
              (labeled-field "Number of pages to fetch" n-text) (labeled-field "Path to output file" out-file-text)
              (hide! (label)) btn]
      :columns 2
      :vgap 5
      :hgap 5
      :border 5)))

(defn make-window []
  (let [f (frame :title "Flat Parser" :minimum-size [400 :by 100] :on-close :dispose )
        main-panel (tabbed-panel :tabs [{:title "Default" :content (default-panel)} {:title "Hemnet" :content (hemnet-panel)}])]
    (config! f :content main-panel)
    (-> f pack! show!)))

(defn run-gui []
  (make-window))


(defn -main [& args]
  (if (empty? args)
    (run-gui)
    (let [[url-p1 n kp out-file] args]
      (run-shell url-p1 n kp out-file))))

;; (-main)