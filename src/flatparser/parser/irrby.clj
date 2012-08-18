(ns flatparser.parser.irrby
  (:refer-clojure)
  (:use [net.cgrand enlive-html]
        [clojure pprint repl]
        [clojure.java io]
        [flatparser geo util]
        [flatparser.parser putil]))


(def subway-coords-mnk
  [[53.9084306 27.4793762] [53.9056244 27.5378537] [53.9153569 27.5828397]
   [53.9388635 27.6670396] [53.9279837 27.6278204] [53.8936547 27.5701582]
   [53.9344835 27.6513112] [53.8623126 27.674346] [53.8690346 27.6474166]
   [53.94535219999999 27.687875] [53.9018129 27.5607115]
   [53.9091227 27.5747555] [53.9064619 27.5213796] [53.9241648 27.6133633]
   [53.9065756 27.4545872] [53.9054617 27.5544432] [53.8896343 27.5856077]
   [53.9095682 27.4985433] [53.8864055 27.53721] [53.8768861 27.6268709]
   [53.9073688 27.4351037] [53.8926937 27.5477886]
   [53.9211795 27.5977689] [53.889564 27.6156753]])


(def param-names
  {:street "Улица"
   :house "Номер дома"
   :room_no "Количество комнат"
   :size_t "Общая площадь"
   :size_l "Жилая площадь"
   :size_k "Площадь кухни"
   :state "Ремонт"
   :floor "Этаж"
   :year_built "Год постройки"
   :floors "Этажность"
   :restroom "Санузел"
   :balcony "Балкон / лоджия"
   :walls "Материал стен"
   :furniture "Мебель"
   })


;; (defn fetch-dist-to-subway [rsrc]
;;   (let [content (get-content (first (select rsrc [:.metroImg])))]
;;     (if (or (nil? content) (empty? content))
;;       NA
;;       (let [dist-str (last (.split content ","))]
;;         (cond 
;;          (or (nil? dist-str) (empty? dist-str)) NA
;;          :else (parse-int (.trim dist-str)))))))


(defn fetch-price [rsrc]
  (let [price-str (first (:content (first (select rsrc [:b#priceSelected]))))]
    (if price-str
      (parse-int (.replaceAll price-str "[^0-9]*" " "))
      NA)))


(defn parse-state [state-str]
  (if state-str
    (case (.toLowerCase state-str)
      "удовлетворительный ремонт" :bad
      "строительная отделка" :bad
      "нормальный ремонт" :normal      
      "хороший ремонт" :good
      "отличный ремонт" :good
      state-str)
    NA))


(defn parse-restroom [restroom-str]
  (if restroom-str
    (case (.toLowerCase restroom-str)
      "раздельный" 1
      "совмещенный" 0
      NA)))

(defn parse-balcony [balcony-str]
  (cond (nil? balcony-str) NA
        (= balcony-str "нет") 0
        :else 1))

(defn parse-dist-to-subway [street house]
  (if street
    (let [coords (coords-by-addr (str "Беларусь, Минск, " street ", " house))]
      (if-not (empty? coords)
        (nearest-subway subway-coords-mnk coords)
        NA))
    NA))
 
(defn parse-walls [walls-str]
  (if walls-str
    (case (.toLowerCase walls-str)
      "панельный"  :panel
      "монолитный" :monolit
      "кирпичный"  :brick
      "блочный"    :block
      "каркасный"  :frame
      NA)))

(defn parse-furniture [furn-str]
  (if furn-str
    (case (.toLowerCase furn-str)
      "нет" 0
      1)
    NA))

(defn fetch-main-params
  "Takes URL of page with flat description and
   returns map with found params"
  [rsrc]
  (let [main (select rsrc [:table#mainParams])
        names (rest (map (comp first :content) (select main [:span])))
        values (map (comp first :content) (select main [:div]))
        p-map (zipmap names values)]
    ;;(println p-map)
    {:dist_to_sub (parse-dist-to-subway (p-map (param-names :street))
                                            (p-map (param-names :house)))
     :room_no (parse-int (p-map (param-names :room_no)))
     :size_t (parse-double (p-map (param-names :size_t)))
     :size_l (parse-double (p-map (param-names :size_l)))
     :size_k (parse-double (p-map (param-names :size_k)))
     :state (parse-state (p-map (param-names :state)))
     :floor (parse-int (p-map (param-names :floor)))
     :floors (parse-int (p-map (param-names :floors)))
     :restroom (parse-restroom (p-map (param-names :restroom)))
     :balcony (parse-balcony (p-map (param-names :restroom)))
     :year_built (parse-int (p-map (param-names :year_built)))
     :walls (parse-walls (p-map (param-names :walls)))
     :furniture (parse-furniture (p-map (param-names :furniture)))
     }))

(defn fetch-params [rsrc]
  (merge
   (fetch-main-params rsrc)
   {:price (fetch-price rsrc)
    :dist_to_kp NA
    :district NA
    :date NA
    :subway NA
    }))



(defn fetch-links [rsrc]
  (map (comp #(str "http://irr.by" %) :href :attrs)
       (select rsrc [:td.tdTxt :div.h3 :a])))



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
  (let [list-pages (cons url-p1 (map #(str url-p1 "/page" % "/")
                                     (range 2 (+ n 1))))]
    (apply concat (map collect-from list-pages))))