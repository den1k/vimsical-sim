(ns vimsical.sim.simulation
  (:require
   [vimsical.sim.scenarios.pen-changes-scenario :as pen]))

(defn new-sim
  [{:keys [rng batch-interval-ms batch-max-count pens]}]
  {:pre [(seq pens)] :post [(seq (:scenarios %))]}
  {:name "Vimsical X Codepen"
   :scenarios
   (mapv
    (partial pen/new-scenario rng batch-interval-ms batch-max-count)
    pens)})
