(ns vimsical.sim.simulation
  (:require
   [vimsical.sim.scenarios.pen-changes-scenario :as pen]))

(defn new-sim
  [rng pens]
  {:pre [(seq pens)] :post [(seq (:scenarios %))]}
  {:name      "Vimsical X Codepen"
   :scenarios (mapv (partial pen/new-scenario rng) pens)})
