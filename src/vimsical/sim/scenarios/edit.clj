(ns vimsical.sim.scenarios.edit
  (:require
   [aleph.http :as http]
   [clojure.core.async.impl.protocols :as p]
   [clojure.core.async :as a]
   [manifold.stream :as s]
   [manifold.deferred :as d]))

;; * Helpers

(defn deferred->chan [d]
  (reify p/ReadPort
    (take! [_ fn1-handler]
      (if (realized? d)
        d
        (d/on-realized d fn1-handler fn1-handler)))))

;; * Step fns

(defn init-step
  "Assoc the ws-conn in the ctx map"
  [{:keys [url] :as ctx}]
  (a/go
    (d/let-flow
        [conn   (http/websocket-connection ws-url)
         result (deferred->chan (s/put! conn changes))]
      [result (assoc ctx ::ws-conn conn)])))

(defn new-step
  [changes]
  (fn step
    [{::keys [ws-conn] :as ctx}]
    (deferred->chan
      (s/put! ws-conn changes))))


;; * Scenario

;; (defn pen-scenario
;;   [{:keys [id code]}]
;;   {:name (str id)
;;    :steps })
