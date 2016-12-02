(ns vimsical.sim.core
  (:require
   [taoensso.timbre :as timbre]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [me.raynes.fs :as fs]
   [vimsical.sim.util.rand :as rand]
   [environ.core :as env]
   [clj-gatling.core :as gatling]
   [vimsical.sim.simulation :as sim]))

;; * Logging

(timbre/set-level! :info)

;; * Env

(defn parse-long [s] (Long/parseLong s))
(defn parse-double [s] (Double/parseDouble s))

(def RNG
  (rand/rng
   (some-> (env/env :rng-seed) (parse-long))))

(def pens-default-dir
  (fs/file (io/resource "pens/")))

(def pens-dir
  (env/env :pens-dir pens-default-dir))

(def pens-limit
  1
  ;; (some-> (env/env :pens-limit) (parse-long))
  )

(def concurrency
  (some-> (env/env :concurrency) (parse-long)))

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
  (let [sim  (sim/new-sim RNG (load-pens-from-dir pens-dir 1))
        opts {:concurrency concurrency
              :context     {:ws-url     "ws://localhost:8080/websocket" ;; (env/env :ws-url)
                            :remote-url (env/env :remote-url)}}]
    (gatling/run sim opts)))

(defn -main [& _] (run-sim!))
