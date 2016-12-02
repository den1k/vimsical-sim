(ns vimsical.sim.simulation
  (:require
   [vimsical.sim.scenarios.pen-changes-scenario :as pen]))

(defn new-sim
  [rng pens]
  {:name      "Vimsical X Codepen"
   :scenarios (mapv pen/new-scenario pens)})
