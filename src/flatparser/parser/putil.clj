
(ns flatparser.parser.putil
  (:refer-clojure)
  (:use [net.cgrand enlive-html]
        [clojure pprint repl]
        [clojure.java io]))

(defn make-resource [url]
  (html-resource (reader url)))

(defn get-content
  "Takes map, representing node, and returns its contents"
  [node]
  (first (:content node)))

