(ns vimsical.sim.scenarios.pen-changes-scenario
  (:require
   [taoensso.timbre :refer [debug]]
   [vimsical.sim.steps.changes :as changes]
   [vimsical.sim.steps.vims-session :as vims-session]
   [vimsical.sim.steps.create-vims :as vims]
   [vimsical.sim.steps.rng :as rng]
   [vimsical.sim.steps.user :as user]
   [vimsical.sim.steps.ws-client :as ws-client]))

(defn new-scenario
  [rng {:keys [id views] :as pen}]
  {:post [(->> % :steps (every? map?))]}
  (let [steps (into
               [rng/rng-step
                user/user-step
                vims/create-vims-step
                ws-client/ws-chan-step
                vims-session/vims-session-step]
               (changes/new-pen-changes-steps rng pen))]
    {:name   id
     :weight (Integer/parseInt views)
     :steps  steps}))
