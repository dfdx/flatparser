
(ns flatparser.core
  (:refer-clojure)
  (:use [clojure pprint repl]
        [clojure.data json]
        [clojure.java io]
        [flatparser util])
  (:require [flatparser.parser
             [rent-n-sale :as rns]
             [irrby :as irrby]
             [bn :as bn]])
  (:gen-class))


(def feat-order
  [:price :clazz :room_no :size_t :size_l :size_k :dist_to_subway :dist_to_kp
   :state :beds :furniture :district :subway :floor :floors :year_built
   :walls :date :url])

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


(defn dataset-from-url [url-p1 n]
  (let [collector 
        (cond
         (.startsWith url-p1 "http://irr.by") irrby/collect-data
         (.startsWith url-p1 "http://irr.tut.by") irrby/collect-data
         (.startsWith url-p1 "http://rent-and-sale.ru") rns/collect-data
         (.startsWith url-p1 "http://www.bn.ru") bn/collect-data
         :else nil)]
    (if collector
      (make-dataset (collector url-p1 n))
      (println "Sorry, I don't know how to parse this site"))))

(defn -main [url-p1 n out-file]
  (write-csv out-file (dataset-from-url url-p1 (parse-int n))))

