(ns vimsical.sim.util.ws
  (:require
   [aleph.http :as http]
   [clojure.core.async :as a]
   [clojure.core.async.impl.protocols :as p]
   [manifold.stream :as s]
   [vimsical.sim.util.manifold :as m]))

;; * Client

(defn new-client-chan
  "High-order channel that will put a Read/Write Port once the websocket
  connection has been established."
  [url options read-chan write-chan]
  (a/go
    (let [client-stream-deferred (http/websocket-client url options)
          client-stream-chan     (m/deferred->chan client-stream-deferred)
          client-stream          (a/<! client-stream-chan)]
      (do
        ;; Create a 2-way connection between the aleph client-stream and our
        ;; internal channels, note that we cannot use the same channel for read
        ;; and write or we'd cause a loop
        (s/connect client-stream read-chan)
        (s/connect write-chan client-stream)
        ;; Reify a read/write port that dispatches to the internal channels
        (reify
          p/ReadPort
          (take! [_ fn1-handler]
            (p/take! read-chan fn1-handler))
          p/WritePort
          (put! [port val fn1-handler]
            (p/put! write-chan val fn1-handler)))))))
