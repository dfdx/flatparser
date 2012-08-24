(ns flatparser.parser.irrby
  (:refer-clojure)
  (:use [net.cgrand enlive-html]
        [clojure pprint repl]
        [clojure.java io]
        [flatparser geo util]
        [flatparser.parser putil]))

(def subway-stations
  ["Институт культуры"
   "Ленина пл."
   "Победы пл."
   "Якуба Коласа пл."
   "Академия наук"
   "Парк Челюскинцев"
   "Московская"
   "Восток"
   "Борисовский тракт"
   "Уручье"
   "Каменная Горка"
   "Кунцевщина"
   "Спортивная"
   "Пушкинская"
   "Молодежная"
   "Фрунзенская"
   "Немига"
   "Октябрьская"
   "Первомайская"
   "Пролетарская"
   "Тракторный завод"
   "Партизанская"
   "Автозаводская"
   "Могилевская"])


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

(defn param [p-map name]
  (p-map (param-names name)))

(defn address [p-map]
  (str (param p-map :street) ", " (param p-map :house)))

(defn parse-dist-to-kp [addr kp]
  (if kp
    (distance (coords-by-addr (str "Беларусь, Минск, " addr))
              (coords-by-addr (str "Беларусь, Минск, " kp)))
    NA))

(defn fetch-params
  "Takes parsed HTML resource of page with flat description
   map of additional information. 
   Returns map with found/calcuated params"
  [rsrc info]
  (let [main (select rsrc [:table#mainParams])
        names (rest (map (comp first :content) (select main [:span])))
        values (map (comp first :content) (select main [:div]))
        p-map (zipmap names values)
        coords (coords-by-addr (str "Беларусь, Минск, " (address p-map)))]
    {:price (fetch-price rsrc)
     :address (address p-map)
     :lat (first coords)
     :lon (second coords)
     :dist_to_sub (parse-dist-to-subway (param p-map :street)
                                        (param p-map :house))
     :dist_to_kp (parse-dist-to-kp (address p-map) (:kp info))
     :room_no (parse-int (param p-map :room_no))
     :size_t (parse-double (param p-map :size_t))
     :size_l (parse-double (param p-map :size_l))
     :size_k (parse-double (param p-map :size_k))
     :state (parse-state (param p-map :state))
     :floor (parse-int (param p-map :floor))
     :floors (parse-int (param p-map :floors))
     :restroom (parse-restroom (param p-map :restroom))
     :balcony (parse-balcony (param p-map :restroom))
     :year_built (parse-int (param p-map :year_built))
     :walls (parse-walls (param p-map :walls))
     :furniture (parse-furniture (param p-map :furniture))
     }))


(defn fetch-links [rsrc]
  (map (comp #(str "http://irr.by" %) :href :attrs)
       (select rsrc [:td.tdTxt :div.h3 :a])))



(defn collect-from
  "Collects data from 1 page of search results"
  [url info]
  (map #(do (println "Fetching params from:" %)
            (merge (fetch-params (make-resource %) info) {:url %}))
       (fetch-links (make-resource url))))

(defn collect-data
  "Takes URL of first page of search results
   and collects data from n first pages"
  [url-p1 n info]
  (let [list-pages (cons url-p1 (map #(str url-p1 "/page" % "/")
                                     (range 2 (+ n 1))))]
    (apply concat (map #(collect-from % info) list-pages))))