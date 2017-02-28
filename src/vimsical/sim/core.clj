(ns vimsical.sim.core
  (:require
   [clj-gatling.core :as gatling]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [environ.core :as env]
   [me.raynes.fs :as fs]
   [taoensso.timbre :as timbre]
   [vimsical.sim.simulation :as sim]
   [vimsical.sim.util.rand :as rand])
  (:import [ch.qos.logback.classic Level Logger]))


;; * Logging

(defn set-log-level!
  [level]
  (timbre/set-level! level)
  (doto ^Logger (org.slf4j.LoggerFactory/getLogger (Logger/ROOT_LOGGER_NAME))
    (.setLevel (case level
                 :info  Level/INFO
                 :debug Level/DEBUG
                 :trace Level/TRACE))))


;; * Env

(defn parse-long   [s] (Long/parseLong s))
(defn parse-double [s] (Double/parseDouble s))


;; * Options

(def RNG (some-> (env/env :rng-seed) (parse-long) (rand/rng)))
(def pens-default-dir (fs/file (io/resource "pens/")))
(def pens-dir (env/env :pens-dir pens-default-dir))
(def pens-limit (some-> (env/env :pens-limit) (parse-long)))
(def concurrency (some-> (env/env :concurrency) (parse-long)))


;; * Sim

(defn load-pens-from-dir
  [dir limit]
  (sequence
   (comp
    (map slurp)
    (map edn/read-string)
    (if limit (take limit) (map identity)))
   (shuffle
    (fs/find-files dir #".+\.edn"))))

(defn run-sim! []
  (timbre/info
   (format "Starting sim with:\n- pens:%s\n- concurrency:%s" pens-limit concurrency))
  (let [pens (load-pens-from-dir pens-dir pens-limit)
        sim  (sim/new-sim
              {:rng               RNG
               :batch-interval-ms 13e3
               :batch-max-count   1e2
               :pens              pens})
        opts {:concurrency   concurrency
              :timeout-in-ms 30000
              :context       {:ws-url (env/env :ws-url) :remote-url (env/env :remote-url)}}]
    (gatling/run sim opts)))


(defn -main [& _]
  (set-log-level! :info)
  (run-sim!))

;; * Stats

;; Getting insights on the average pen size

(comment
  (defn pen-code
    [pen type]
    (get-in pen [:code type]))

  (defn pen-chars
    [pen]
    (reduce + (map (comp count (partial pen-code pen)) [:html :css :js])))

  (defn scale-count
    [cnt]
    (let [mv    (partial * 2)
          typos (comp mv (partial * 0.05))
          edits (comp mv (partial * 0.1))]
      (as-> cnt c
        (+ c (mv c))
        (+ c (typos c))
        (+ c (edits c))
        (long c))))

  (defn bucket [size n]
    (long (num (/ n size))))

  (defn bucket-coll [size coll]
    (let [freqs (frequencies (map (partial bucket size) coll))
          total (count coll)]
      (reduce-kv
       (fn [m k v]
         (assoc m k {:n v :pct (* 100 (double (/ v total)))}))
       (sorted-map-by >) freqs)))

  ;; (spit
  ;;  (bucket-coll 1e4 (map pen-chars (load-pens-from-dir pens-dir nil))))


  (def buckets
    {94 1,
     74 2,
     71 2,
     64 1,
     63 1,
     62 2,
     59 1,
     54 1,
     53 1,
     51 1,
     50 1,
     49 4,
     47 3,
     46 2,
     45 2,
     44 4,
     43 4,
     42 5,
     41 7,
     40 2,
     39 4,
     38 4,
     37 2,
     36 6,
     35 6,
     34 7,
     33 7,
     32 7,
     31 7,
     30 5,
     29 6,
     28 9,
     27 7,
     26 3,
     25 14,
     24 14,
     23 7,
     22 15,
     21 13,
     20 8,
     19 17,
     18 17,
     17 14,
     16 16,
     15 21,
     14 37,
     13 25,
     12 30,
     11 46,
     10 48,
     9 59,
     8 72,
     7 93,
     6 129,
     5 175,
     4 255,
     3 406,
     2 841,
     1 2905,
     0 29120})
  (binding [*out* (clojure.java.io/writer "/Users/julien/projects/vimsical-sim/pens-size-10k-buckets.edn")]
    (clojure.pprint/pprint (bucket-coll 1e4 (map pen-chars (load-pens-from-dir pens-dir nil))))
    (flush)))
