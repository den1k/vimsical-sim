(ns vimsical.sim.steps.vims-session
  (:require
   [clojure.core.async :as a]
   [taoensso.timbre :refer [debug error]]))

(defn ctx->tx
  [{:keys [app-user-id vims-id]}]
  {:pre [(number? app-user-id) (number? vims-id)]}
  [(list 'vims/new-session
         {:store.sync.protocol/user-id app-user-id
          :store.sync.protocol/vims-id vims-id})])

(defn valid-resp?
  [[[_ {:keys
        [store.sync.protocol/token
         store.sync.protocol/primary-keys]}]]]
  (and token (empty? primary-keys)))

(defn merge-resp
  [ctx [[_ {:keys [store.sync.protocol/token]}]]]
  {:post [(some? (:token %))]}
  (assoc ctx :token token))

(defn create-vims-session-step-fn
  [{:keys [ws-chan] :as ctx}]
  {:pre [ws-chan]}
  (a/go
    (debug "Step")
    (try
      (let [tx   (ctx->tx ctx)
            _    (debug tx)
            _put (a/>! ws-chan tx)
            _    (debug "put" _put)
            [resp _] (a/alts! [ws-chan (a/timeout 10000)])]
        (debug "valid" (valid-resp? resp) resp)
        [(valid-resp? resp) (merge-resp ctx resp)])
      (catch Throwable t
        (error t)))))

(def vims-session-step
  {:name    "Start a vims session"
   :request create-vims-session-step-fn})
