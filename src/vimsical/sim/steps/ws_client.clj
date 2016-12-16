(ns vimsical.sim.steps.ws-client
  (:require
   [clojure.core.async :as a]
   [taoensso.encore :refer [uuid-str]]
   [taoensso.sente :as sente]
   [taoensso.sente.packers.transit :as sente-transit]
   [taoensso.timbre :refer [debug error]]
   [vimsical.sim.util.transit :as t]
   [vimsical.sim.util.ws :as ws]))

;; * Sente compat

(def transit-packer
  (sente-transit/->TransitPacker :json t/writer-opts t/reader-opts))

(defn valid-handshake?
  [[k]]
  (= :chsk/handshake k))

(defn sente-pack
  [msg]
  (#'sente/pack transit-packer {} [:websocket/default msg]))

(defn sente-unpack
  [s]
  (let [resp (#'sente/unpack transit-packer s)]
    (debug "resp:" resp)
    (if (vector? resp)
      (first resp)
      resp)))


;; * Events

(def ignored-events #{:chsk/ws-ping})
(def error-events #{:store.sync.protocol.response/error})

(defn matches-event?
  [e events]
  (cond
    (keyword? e) (contains? events e)
    (vector? e)  (matches-event? (first e) events)
    :else        (assert false)))

(defn poll-error
  [conn-chan]
  (when-let [e (a/poll! conn-chan)]
    (if (matches-event? e error-events)
      e
      (debug "Ignoring event" e))))


;; * Client

(def default-buffer-size 1024)

(defn ws-chan-step-fn
  [{:keys [ws-url headers user-id] :as ctx}]
  {:pre [ws-url headers]}
  (a/go
    (try
      (debug "WS" ctx)
      (let [err         (fn [e] (error e))
            url         (format "%s?client-id=%s" ws-url (uuid-str))
            read-chan   (a/chan
                         default-buffer-size
                         (comp
                          (map sente-unpack)
                          (map (fn [e] (debug e) e))
                          (remove (fn [e] (matches-event? e ignored-events))))
                         err)
            write-chan  (a/chan default-buffer-size (map sente-pack) err)
            client-chan (ws/new-client-chan url {:headers headers} read-chan write-chan)
            conn-chan   (a/<! client-chan)
            handshake   (a/<! conn-chan)]
        (debug "Conn: valid?" (valid-handshake? handshake) conn-chan handshake)
        [(valid-handshake? handshake) (assoc ctx :ws-chan conn-chan)])
      (catch Throwable t
        (error t)))))

;; * Steps

(def ws-chan-step
  {:name    "Assoc :ws-chan in the ctx map"
   :request ws-chan-step-fn})


