(ns vimsical.sim.steps.vims-session
  (:require
   [clojure.core.async :as a]
   [vimsical.sim.util.uuid :as uuid]
   [taoensso.timbre :refer [debug error]]))

(defn ctx->tx
  [{:keys [app-user-uuid vims-uuid]}]
  {:pre [(uuid/uuid? app-user-uuid) (uuid/uuid? vims-uuid)]}
  [(list 'vims/new-session
         {:store.sync.protocol/user-uuid app-user-uuid
          :store.sync.protocol/vims-uuid vims-uuid})])

(defn valid-resp?
  [resp]
  (try
    (let [[[_ {:keys
               [store.sync.protocol/token
                store.sync.protocol/primary-keys]}]] resp]
      (and token (empty? primary-keys)))
    (catch Throwable t
      (error t resp))))

(defn merge-resp
  [ctx resp]
  (try
    (let [[[_ {:keys [store.sync.protocol/token]}]] resp]
      (debug "token" token)
      (assoc ctx :token token))
    (catch Throwable t
      (error t resp))))

(defn create-vims-session-step-fn
  [{:keys [ws-chan] :as ctx}]
  {:pre [ws-chan]}
  (a/go
    (try
      (debug "Step")
      (let [tx   (ctx->tx ctx)
            _put (a/>! ws-chan tx)
            resp (a/<! ws-chan)]
        (when-not (valid-resp? resp)
          (error "invalid resp" resp tx))
        [(valid-resp? resp) (merge-resp ctx resp)])
      (catch Throwable t
        (a/close! ws-chan)
        (error t)))))

(def vims-session-step
  {:name    "Start a vims session"
   :request create-vims-session-step-fn})
