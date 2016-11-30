(defproject vimsical-sim "0.1.0-SNAPSHOT"
  :dependencies
  [[org.clojure/clojure       "1.8.0"]     ; aleph doesn't support 1.9
   [org.clojure/core.async    "0.2.395"]
   [aleph                     "0.4.1"]
   [cheshire                  "5.6.3"]
   [clj-gatling               "0.8.3"]
   [com.cognitect/transit-clj "0.8.295"]
   [environ                   "1.1.0"]
   [org.omcljs/om             "1.0.0-alpha47"]
   [me.raynes/fs              "1.4.6"]
   [org.clojure/clojurescript "1.9.93"]]
  :plugins
  [[lein-environ "1.1.0"]])