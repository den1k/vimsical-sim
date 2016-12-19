(ns vimsical.sim.steps.rng
  (:require
   [clojure.core.async :as a]
   [taoensso.timbre :refer [debug]]
   [vimsical.sim.util.rand :as rand])
  (:import
   (java.util Random)))

;; * RNG

(defn new-rng-step-fn
  [^Random rng]
  (fn rng-step-fn
    [{:keys [user-id] :as ctx}]
    {:pre  [user-id]}
    (a/go
      (debug "Step" ctx)
      [true (assoc ctx :rng (rand/rng (.nextLong rng)))])))

(defn new-rng-step
  [rng]
  {:name    "Setup ctx RNG"
   :request (new-rng-step-fn rng)})
