
(ns flatparser.util
  (:refer-clojure)
  (:use [clojure repl pprint]))


(def NA "NA")

(defn take-num [s]
  (apply str (take-while #(or (Character/isDigit %) (= % \.) (= % \,)) s)))

(defn parse-int [s]
  (cond (or (nil? s) (empty? (.trim s))) NA
        (not (Character/isDigit (first (.trim s)))) NA
        :else (read-string (take-num (.replaceAll
                                      (.replaceAll (.trim s) "," "")
                                      " " "")))))

(defn parse-double [s]
  (cond (or (nil? s) (empty? (.trim s))) NA
        (not (Character/isDigit (first (.trim s)))) NA
        :else (read-string (take-num (.replaceAll
                                      (.replaceAll (.trim s) "," ".")
                                      " " "")))))

(defn replace-str [smap s]
  (loop [s s, smap smap]
    (cond (empty? smap) s
          :else (recur (.replaceAll s (ffirst smap) (second (first smap)))
                       (rest smap)))))
