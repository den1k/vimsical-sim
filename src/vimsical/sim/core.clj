(ns vimsical.sim.core
  (:require
   [taoensso.timbre :as timbre]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [me.raynes.fs :as fs]
   [vimsical.sim.util.rand :as rand]
   [environ.core :as env]
   [clj-gatling.core :as gatling]
   [vimsical.sim.simulation :as sim])
  (:import
   (ch.qos.logback.classic Level Logger)))

;; * Logging

(defn set-log-level!
  [level]
  (timbre/set-level! :info)
  (doto ^Logger (org.slf4j.LoggerFactory/getLogger (Logger/ROOT_LOGGER_NAME))
    (.setLevel (case level :info  Level/INFO :debug Level/DEBUG))))


;; * Env

(defn parse-long [s] (Long/parseLong s))
(defn parse-double [s] (Double/parseDouble s))

(def RNG
  (rand/rng
   (some-> (env/env :rng-seed) (parse-long))))

(def pens-default-dir
  (fs/file (io/resource "pens/")))

(def pens-dir    (env/env :pens-dir pens-default-dir))
(def pens-limit  (some-> (env/env :pens-limit) (parse-long)))
(def concurrency (some-> (env/env :concurrency) (parse-long)))


;; * Sim

(defn load-pens-from-dir
  [dir limit]
  (sequence
   (comp
    (map slurp)
    (map edn/read-string)
    (if limit (take limit) (map identity)))
   (fs/find-files dir #".+\.edn")))

(defn run-sim!
  []
  (set-log-level! :info)
  (timbre/info
   (format "Starting sim with:\n- pens:%s\n- concurrency:%s" pens-limit concurrency))
  (let [pens (shuffle (load-pens-from-dir pens-dir 1 pens-limit))
        sim  (sim/new-sim RNG pens)
        opts {:concurrency   concurrency
              :timeout-in-ms 30000
              :context       {:ws-url     (env/env :ws-url)
                              :remote-url (env/env :remote-url)}}]
    (gatling/run sim opts)))

(defn -main [& _]
  (set-log-level! :info)
  (run-sim!))
