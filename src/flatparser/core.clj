(ns flatparser.core
  (:refer-clojure)
  (:use [net.cgrand enlive-html]
        [clojure pprint repl]
        [clojure.data json]
        [clojure.java io]
        [flatparser parser geo])
  (:gen-class))


(defn safe-parse-int [s subst]
  (try
    (Integer/parseInt (.replaceAll s "," "."))
    (catch Exception e
      subst)))

(defn safe-parse-double [s subst]
  (try
    (Double/parseDouble (.replaceAll s "," "."))
    (catch Exception e
      subst)))

(defn year [year-str]
  (cond
   (nil? year-str) 1980
   (= (.length year-str) 2) (+ (safe-parse-int year-str 80) 1900)
   (= (.length year-str) 4) (safe-parse-int year-str 1980)
   (> (.length year-str) 4) (safe-parse-int (.substring year-str 0 4) 1980)
   :else 1980))

(defn restroom-type [flat]
  (if (flat "Санузел")
    (let [restroom (.toLowerCase (flat "Санузел"))]
      (cond (= restroom "раздельный") {:restroom-sep 1, :restroom-com 0}
            (= restroom "совмещённый") {:restroom-sep 0, :restroom-com 1}
            :else {:restroom-sep 0, :restroom-com 0}))
    {:restroom-sep 0, :restroom-com 0}))

(defn distance-to-subway [flat]
  (let [station (flat "Станция метро")
        street (flat "Улица")
        building-no (flat "Номер дома")]
    (if (and station street building-no)
      (distance (coords-by-addr (str "Беларусь, Минск, " street ", "
                                     building-no))
                (subway-coords station))
      2000)))

(defn balcony [flat]
  (let [balc (flat "Балкон / лоджия")]
    (cond (nil? balc) 0
          (= balc "нет") 0
          :else 1)))

(defn parse-space [space-str subst]
  (if (nil? space-str)
    subst
    (safe-parse-double space-str subst)))

(defmacro or-1 [condition & body]
  `(let [result# ~condition]
     (if (= result# -1)
       ~@body
       result#)))


(defn features [flat]
  (merge
   (sorted-map)
   {:url (flat :url)
    :price (or (flat "Цена") -1)
    :age (- 2012 (year (flat "Год постройки")))
    :dist-to-subway (distance-to-subway flat)
    :storey (safe-parse-int (flat "Этаж") 4)
    :storey-no (safe-parse-int (flat "Этажность") 7)
    :balcony (balcony flat)
    :total-space (parse-space (flat "Общая площадь") -1)
    :living-space (parse-space (flat "Жилая площадь") -1)
    :kitchen-space (parse-space (flat "Площадь кухни") 7)
    :room-no (safe-parse-int (flat "Количество комнат") -1)
    }
   (restroom-type flat)))


(defn write-csv [filepath feats]
  (with-open [wr (writer filepath)]
    (cl-format wr "~{~a~^,~}~%" (map #(.replaceAll (name %) "-" "_")
                                   (keys (first feats))))
    (doseq [feat feats]
      (cl-format wr "~{~a~^,~}~%" (vals feat)))))

(defn make-dataset [list-url]
  (filter #(and (not= (:price %) -1) (not= (:total-space %) -1)
                (not= (:living-space %) -1) (not= (:room-no %) -1))
          (map #(do (Thread/sleep 100) (features %)) (parse-list list-url))))

(defn -main [list-url out-file]
  (write-csv out-file (make-dataset list-url)))