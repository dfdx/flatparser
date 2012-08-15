
(ns flatparser.parser.rent-n-sale
  (:refer-clojure)
  (:use [net.cgrand enlive-html]
        [clojure pprint repl]
        [clojure.java io]
        [flatparser util]
        [flatparser.parser putil]))


(def param-names
  {:clazz "Класс:"
   :room_no "Количество комнат:"
   :size_t "Общая площадь:"
   :size_l "Жилая площадь:"
   ;; size of rooms may go here
   :size_k "Площадь кухни:"
   :state "Состояние:"
   :floor "Этаж:"
   :year_built "Год постройки:"
   :floors "Этажей:"
   :furniture "Мебель:"
   :beds "Спальных мест:"
   })


(defn fetch-dist-to-subway [rsrc]
  (let [content (get-content (first (select rsrc [:.metroImg])))]
    (if (or (nil? content) (empty? content))
      NA
      (let [dist-str (last (.split content ","))]
        (cond 
         (or (nil? dist-str) (empty? dist-str)) NA
         :else (parse-int (.trim dist-str)))))))


;; I've never seen price in currency other than rubs/month
;; but in theory it may occur, so this function may need to be modified
(defn fetch-price [rsrc]
  (parse-int (get-content
              (first (select rsrc [:table.headerValueTable :td :span])))))

(defn parse-clazz [clazz-str]
  (case (.toLowerCase clazz-str)
    "эконом" :econom
    "бизнес" :business
    "элитный" :elite
    NA))

(defn parse-state [state-str]
  (if (nil? state-str)
    NA
    (case (.toLowerCase state-str)
      "нормальное состояние" :normal
      "после ремонта" :good
      "евро ремонт" :good
      "авторский дизайн" :good
      "нужен капитальный ремонт" :bad
      "нужен косметический ремонт" :bad
      "без отделки" :bad
      "с отделкой" :good
      NA)))

(defn parse-furniture [furniture-str]
  (case (.toLowerCase furniture-str)
    "нет" 0
    "есть" 1
    NA))


(defn fetch-main-params
  "Takes URL of page with flat description and
   returns map with found params"
  [rsrc]
  (let [names (map get-content (select rsrc [:div.x2 :th]))
        values (map get-content (select rsrc [:div.x2 :td]))
        p-map (zipmap names values)]
    {:clazz (parse-clazz (p-map (param-names :clazz)))
     :room_no (parse-int (p-map (param-names :room_no)))
     :size_t (parse-double (p-map (param-names :size_t)))
     :size_l (parse-double (p-map (param-names :size_l)))
     ;; size of rooms may go here
     :size_k (parse-double (p-map (param-names :size_k)))
     :state (parse-state (p-map (param-names :state)))
     :floor (parse-int (p-map (param-names :floor)))
     :floors (parse-int (p-map (param-names :floors)))
     :year_built (parse-int (p-map (param-names :year_built)))
     :furniture (parse-furniture (p-map (param-names :furniture)))
     :beds (parse-int (p-map (param-names :beds)))
     }))

(defn fetch-params [rsrc]
  (merge
   (fetch-main-params rsrc)
   {:price (fetch-price rsrc)
    :dist_to_subway (fetch-dist-to-subway rsrc)
    :dist_to_kp NA
    :district NA
    :date NA
    :subway NA
    }))



(defn fetch-links [rsrc]
  (map (comp #(str "http://rent-and-sale.ru" %) :href :attrs)
       (select rsrc [:div.photo :a])))


(defn collect-from
  "Collects data from 1 page of search results"
  [url]
  (map #(do (println "Fetching params from:" %)
            (merge (fetch-params (make-resource %)) {:url %}))
       (fetch-links (make-resource url))))

(defn collect-data
  "Takes URL of first page of search results
   and collects data from n first pages"
  [url-p1 n]
  (let [list-pages (cons url-p1 (map #(str url-p1 "&pageno" %)
                                     (range 1 n)))]
    (apply concat (map collect-from list-pages))))