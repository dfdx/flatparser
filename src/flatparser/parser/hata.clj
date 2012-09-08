
(ns flatparser.parser.hata
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
  {:address "Адрес:"
   :size_t "Площадь:"
   :state "Состояние:"
   :year_built "Год постройки:"
   :floors "Этаж / этажность:"
   :walls "Материал стен:"
   :kind "Вид объекта:"
   })



(defn fetch-price [rsrc]
  (let [strs (map (comp first :content) (select rsrc [:div.prices :p :span]))
        price-strs (filter #(.startsWith % "€") strs)]
    (if price-strs
      (first (filter (fn [p] (< p 100))
                     (map #(parse-int (.replaceAll % "€" "")) price-strs)))
      NA)))


(defn parse-state [state-str]
  (if state-str
    (case (.toLowerCase state-str)
      "евроремонт" :good
      "косметический ремонт" :normal
      "удовлетворительное" :normal      
      "требуется ремонт" :bad
      "аварийное состояние" :bad
      "строительная отделка" :bad
      "без отделки" :bad
      NA)
    NA))

(defn parse-balcony [balcony-str]
  (cond (nil? balcony-str) NA
        (= balcony-str "нет") 0
        :else 1))

(defn parse-dist-to-subway [addr-str]
  (if addr-str
    (let [coords (coords-by-addr (str "Беларусь, Минск, " addr-str))]
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
      NA)))

(defn parse-furniture [furn-str]
  (if furn-str
    (case (.toLowerCase furn-str)
      "нет" 0
      1)
    NA))

(defn parse-floors [floor-str]
  (if floor-str
    (vec (map parse-int (.split floor-str "/")))
    [NA NA]))

(defn parse-dist-to-kp [addr kp]
  (if kp
    (distance (coords-by-addr (str "Беларусь, Минск, " addr))
              (coords-by-addr (str "Беларусь, Минск, " kp)))
    NA))

(defn param [p-map name]
  (p-map (param-names name)))


(defn fetch-params
  "Takes parsed HTML resource of page with flat description
   map of additional information. 
   Returns map with found/calcuated params"
  [rsrc info]
  (let [trs (select rsrc [:table.typical :tbody :tr])
        td-pairs (map #(select % [:td]) trs)
        p-list (map (fn [[td1 td2]]
                      {(first (:content td1))
                       ((comp first :content first :content) td2)}) td-pairs)
        p-map (reduce into p-list)
        coords (coords-by-addr (str "Беларусь, Минск, "
                                    (param p-map :address)))
        floors (parse-floors (param p-map :floors))]
    {:price (fetch-price rsrc)
     :address (param p-map :address)
     :lat (first coords)
     :lon (second coords)
     :dist_to_subway (parse-dist-to-subway (param p-map :address))
     :dist_to_kp (parse-dist-to-kp (param p-map :address) (:kp info))
     :size_t (parse-double (param p-map :size_t))
     :state (parse-state (param p-map :state))
     :floor (first floors)
     :floors (second floors)
     :year_built (parse-int (param p-map :year_built))
     :walls (parse-walls (param p-map :walls))
     :kind (or (param p-map :kind) NA)
     }))


(defn fetch-links [rsrc]
  (map (comp #(str "http://www.hata.by" %) :href :attrs)
       (select rsrc [:p.title :a])))



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
  (let [base-link (subs url-p1 0 (- (count url-p1) 2))
        list-pages (map #(str base-link % "/") (range 1 (+ n 1)))]
    (apply concat (map #(collect-from % info) list-pages))))
