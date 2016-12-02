(ns vimsical.sim.scenarios.pen-changes-scenario
  (:require
   [vimsical.sim.steps.ctx :as ctx]
   [vimsical.sim.steps.user :as user]
   [vimsical.sim.steps.create-vims :as vims]
   [vimsical.sim.steps.changes :as changes]))

(defn new-scenario
  [{:keys [id views] :as pen}]
  {:name   id
   :weight (Integer/parseInt views)
   :steps  [ctx/rng-step
            user/user-step
            vims/create-vims-step
            ctx/ws-chan-step
            (changes/new-pen-changes-step pen)]})
