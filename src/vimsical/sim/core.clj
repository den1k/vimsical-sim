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
