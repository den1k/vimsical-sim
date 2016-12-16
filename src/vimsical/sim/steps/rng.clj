(ns vimsical.sim.steps.rng
  (:require
   [clojure.core.async :as a]
   [taoensso.timbre :refer [debug]]
   [vimsical.sim.util.rand :as rand]))

;; * RNG

(defn rng-step-fn
  [{:keys [user-id] :as ctx}]
  {:pre  [user-id]}
  (a/go
    (debug "Step" ctx)
    [true (assoc ctx :rng (rand/rng user-id))]))

(def rng-step
  {:name    "Setup ctx RNG"
   :request rng-step-fn})

