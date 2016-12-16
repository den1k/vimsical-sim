(ns vimsical.sim.util.ws
  (:require
   [taoensso.timbre :refer [error]]
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
    (try
      (let [client-stream-deferred (http/websocket-client url options)
            client-stream-chan     (m/deferred->chan client-stream-deferred)
            client-stream          (a/<! client-stream-chan)
            opts                   {:downstream? true :upstream? true}]
        (do
          ;; Create a 2-way connection between the aleph client-stream and our
          ;; internal channels, note that we cannot use the same channel for read
          ;; and write or we'd cause a loop
          (s/connect
           (s/->source client-stream)
           (s/->sink read-chan)
           opts)
          (s/connect
           (s/->source write-chan)
           (s/->sink client-stream)
           opts)
          ;; Reify a read/write port that dispatches to the internal channels
          (reify
            p/Channel
            (closed? [_]
              (or (p/closed? read-chan)
                  (p/closed? write-chan)))
            (close! [_]
              (p/close! read-chan)
              (p/close! write-chan))

            p/ReadPort
            (take! [_ fn1-handler]
              (try
                (p/take! read-chan fn1-handler)
                (catch Throwable t
                  (error t)
                  (throw t))))
            p/WritePort
            (put! [port val fn1-handler]
              (try
                (p/put! write-chan val fn1-handler)
                (catch Throwable t
                  (error t)
                  (throw t)))))))
      (catch Throwable t
        (error t)))))


(comment
  ;; Stream x chan interop
  (let [s (s/stream)
        c (a/chan)]
    (s/connect s c)
    (s/connect c s)
    ;; Stream -> chan
    (s/put! s 1)
    (a/<!! c)
    ;; Chan -> stream
    (a/>!! c 1)
    (s/take! s)))

(comment
  (do
    (require '[manifold.deferred :as d])
    (defn echo-handler
      [req]
      (-> (http/websocket-connection req)
          (d/chain
           (fn [socket]
             (s/connect socket socket)))))

    (def port 10000)
    (defonce server (http/start-server echo-handler {:port port}))
    (def client
      (a/<!!
       (new-client-chan
        (format "ws://localhost:%s" port)
        {}
        (a/chan 1)
        (a/chan 1))))
    (a/>!! client "a")
    (a/<!! client)))
