(ns flatparser.geo
  (:refer-clojure)
  (:use [clojure pprint repl]
        [clojure.data json]))


(def geocode-base-url
  "http://maps.google.com/maps/api/geocode/json?sensor=false&address=")

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

(defn coords-by-addr [addr]
  (let [resp (slurp (str geocode-base-url (java.net.URLEncoder/encode addr)))]
    (map second ((comp :location :geometry first :results) (read-json resp)))))


(def subway-coords
  (apply merge
         (map (fn [station]
                (Thread/sleep 1000)
                {station
                 (coords-by-addr
                  (str "Беларусь, Минск, станция метро " station))})
              subway-stations)))


(defn to-meters [degrees]
  (* degrees 111111))

(defn to-degrees [meters]
  (/ meters 111111))

(defn distance [[x1 y1] [x2 y2]]
  (to-meters (Math/sqrt (+ (Math/pow (- x2 x1) 2) (Math/pow (- y2 y1) 2)))))

