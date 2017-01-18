(ns vimsical.sim.scenarios.pen-changes-scenario
  (:require
   [vimsical.sim.steps.changes :as changes]
   [vimsical.sim.steps.create-vims :as vims]
   [vimsical.sim.steps.rng :as rng]
   [vimsical.sim.steps.user :as user]
   [vimsical.sim.steps.vims-session :as vims-session]
   [vimsical.sim.steps.ws-client :as ws-client]))


(defn new-scenario
  [rng batch-interval-ms batch-max-count {:keys [id views] :as pen}]
  {:post [(->> % :steps (every? map?))]}
  {:name   id
   :weight (Integer/parseInt views)
   :steps  (concat
            ;; Pre
            [(rng/new-rng-step rng)
             user/user-step
             vims/create-vims-step
             ws-client/ws-conn-step
             vims-session/vims-session-step]
            ;; Changes
            (changes/new-pen-changes-steps rng batch-interval-ms batch-max-count pen)
            ;; Cleanup
            [ws-client/ws-cleanup-step])})
