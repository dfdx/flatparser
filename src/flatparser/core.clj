
(ns flatparser.core
  (:refer-clojure)
  (:use [clojure pprint repl]
        [clojure.data json]
        [clojure.java io]
        [flatparser util])
  (:require [flatparser.parser
             [rent-n-sale :as rns]
             [irrby :as irrby]])
  (:gen-class))



;; (defn year [year-str]
;;   (cond
;;    (nil? year-str) 1980
;;    (= (.length year-str) 2) (+ (safe-parse-int year-str 80) 1900)
;;    (= (.length year-str) 4) (safe-parse-int year-str 1980)
;;    (> (.length year-str) 4) (safe-parse-int (.substring year-str 0 4) 1980)
;;    :else 1980))

;; (defn restroom-type [flat]
;;   (if (flat "Санузел")
;;     (let [restroom (.toLowerCase (flat "Санузел"))]
;;       (cond (= restroom "раздельный") {:restroom-sep 1, :restroom-com 0}
;;             (= restroom "совмещённый") {:restroom-sep 0, :restroom-com 1}
;;             :else {:restroom-sep 0, :restroom-com 0}))
;;     {:restroom-sep 0, :restroom-com 0}))

;; (defn distance-to-subway [flat]
;;   (let [station (flat "Станция метро")
;;         street (flat "Улица")
;;         building-no (flat "Номер дома")]
;;     (if (and station street building-no)
;;       (distance (coords-by-addr (str "Беларусь, Минск, " street ", "
;;                                      building-no))
;;                 (subway-coords station))
;;       2000)))

;; (defn balcony [flat]
;;   (let [balc (flat "Балкон / лоджия")]
;;     (cond (nil? balc) 0
;;           (= balc "нет") 0
;;           :else 1)))

;; (defn parse-space [space-str subst]
;;   (if (nil? space-str)
;;     subst
;;     (safe-parse-double space-str subst)))

;; (defmacro or-1 [condition & body]
;;   `(let [result# ~condition]
;;      (if (= result# -1)
;;        ~@body
;;        result#)))


;; (defn features [flat]
;;   (merge
;;    (sorted-map)
;;    {:url (flat :url)
;;     :price (or (flat "Цена") -1)
;;     :age (- 2012 (year (flat "Год постройки")))
;;     :dist-to-subway (distance-to-subway flat)
;;     :storey (safe-parse-int (flat "Этаж") 4)
;;     :storey-no (safe-parse-int (flat "Этажность") 7)
;;     :balcony (balcony flat)
;;     :total-space (parse-space (flat "Общая площадь") -1)
;;     :living-space (parse-space (flat "Жилая площадь") -1)
;;     :kitchen-space (parse-space (flat "Площадь кухни") 7)
;;     :room-no (safe-parse-int (flat "Количество комнат") -1)
;;     }
;;    (restroom-type flat)))

;; (defn make-dataset [list-url]
;;   (filter #(and (not= (:price %) -1) (not= (:total-space %) -1)
;;                 (not= (:living-space %) -1) (not= (:room-no %) -1))
;;           (map #(do (Thread/sleep 100) (features %)) (parse-list list-url))))



(def feat-order
  [:price :clazz :room_no :size_t :size_l :size_k :dist_to_subway :dist_to_kp
   :state :beds :furniture :district :subway :floor :floors :year_built
   :date :url])

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
         :else nil)]
    (if collector
      (make-dataset (collector url-p1 n))
      (println "Sorry, I don't know how to parse this site"))))

(defn -main [url-p1 n out-file]
  (write-csv out-file (dataset-from-url url-p1 (parse-int n))))

