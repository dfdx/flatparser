
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
             [hemnet :as hemnet]])
  (:gen-class))


(def feat-order
  [:price :clazz :room_no :size_t :size_l :size_k :dist_to_subway :dist_to_kp
   :state :beds :furniture :district :subway :floor :floors :year_built
   :walls :date :address :lat :lon :url])

(defn map-values [order feats]
  (vec (map #(or (feats %) NA) order)))

(defn make-dataset
  "Takes sequence of maps, representing data, and produces sequence
   where first element is vector of attribute names
   and all the others are vectors of corresponding features.
   This is mostly to give some order to attributes"
  [maps]
  (cons feat-order (map #(map-values feat-order %) maps)))


(defn write-csv [filepath dataset]
  (with-open [wr (writer filepath)]
    (cl-format wr "~{~a~^,~}~%" (map #(.replaceAll (name %) "-" "_")
                                     (first dataset)))
    (doseq [obs (rest dataset)]
      (cl-format wr "~{~a~^,~}~%" obs))))


(defn make-collector [url-p1]
  (cond
   (.startsWith url-p1 "http://irr.by") irrby/collect-data
   (.startsWith url-p1 "http://irr.tut.by") irrby/collect-data
   (.startsWith url-p1 "http://rent-and-sale.ru") rns/collect-data
   (.startsWith url-p1 "http://www.bn.ru") bn/collect-data
   (.startsWith url-p1 "http://www.hemnet.se") hemnet/collect-data
   :else nil))

;; shell

(defn run-shell [url-p1 n kp out-file]
  (write-csv out-file
             (make-dataset ((make-collector url-p1) url-p1 (parse-int n)
                            {:kp kp}))))

;; GUI

(native!)

(defn btn-listener [url-text n-text kp-text out-file-text]
  (write-csv
   (text out-file-text)
   (make-dataset ((make-collector (text url-text)) (text url-text)
                  (parse-int (text n-text)) {:kp (text kp-text)})))
  (alert (str "Dataset written to " (text out-file-text))))

(defn make-window []
  (let [f (frame :title "Flat Parser" :minimum-size [400 :by 100])
        url-text (text :text "URL of search result page")
        n-text (text "# of pages to fetch")
        out-file-text (text "Path to output file")
        kp-text (text "Key point (address)")
        btn (button :text "Parse" :halign :center)
        main-panel (vertical-panel
                    :items [url-text n-text kp-text out-file-text btn])]    
    (config! f :content main-panel)
    (listen btn :action (fn [e] (btn-listener url-text n-text
                                              kp-text out-file-text)))
    (-> f pack! show!)))

(defn run-gui []
  (make-window))


(defn -main [& args]
  (if (empty? args)
    (run-gui)
    (let [[url-p1 n kp out-file] args]
      (run-shell url-p1 n kp out-file))))

