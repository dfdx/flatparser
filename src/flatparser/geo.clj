
(ns flatparser.geo
  (:refer-clojure)
  (:use [clojure pprint repl]
        [clojure.data json]
        [flatparser util]))


(def geocode-base-url
  "http://maps.google.com/maps/api/geocode/json?sensor=false&address=")


(defn coords-by-addr [addr]
  (let [resp (slurp (str geocode-base-url (java.net.URLEncoder/encode addr)))]
    (map second ((comp :location :geometry first :results) (read-json resp)))))


(defn to-meters [degrees]
  (* degrees 111111))

(defn to-degrees [meters]
  (/ meters 111111))

(defn distance [coords1 coords2]
  (if (and (not (empty? coords1)) (not (empty? coords2)))
    (let [[x1 y1] coords1, [x2 y2] coords2]
      (to-meters (Math/sqrt (+ (Math/pow (- x2 x1) 2)
                               (Math/pow (- y2 y1) 2)))))
    NA))


(defn nearest-subway
  "Given list of subway coordinates and coordinates of some point
   returns distance to nearest subway"
  [subways coords]
  (if-not (empty? coords)
    (apply min (map #(distance % coords) subways))
    NA))