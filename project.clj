(defproject flatparser "1.0.0-SNAPSHOT"
  :description "Flat feature extractor for irr.by and other apartments' search sites"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojars.sritchie09/enlive "1.2.0-alpha1"]
                 [org.clojure/data.json "0.1.2"]
                 [seesaw "1.4.0"]
                 [org.eclipse.jetty/jetty-util "8.1.5.v20120716"]
                 [clj-http "0.5.3"]]
  :main flatparser.core)