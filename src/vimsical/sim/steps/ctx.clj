(ns vimsical.sim.steps.ctx
  (:require
   [taoensso.timbre :refer [error]]
   [taoensso.sente.interfaces :as sente-interfaces]
   [taoensso.sente.packers.transit :as sente-transit]
   [taoensso.sente :as sente]
   [taoensso.encore :refer [uuid-str]]
   [clojure.string :as str]
   [vimsical.sim.util.transit :as t]
   [taoensso.timbre :refer [debug error]]
   [environ.core :as env]
   [clojure.core.async :as a]
   [vimsical.sim.util.ws :as ws]
   [vimsical.sim.util.rand :as rand]))

;; * Sente compat

(def transit-packer
  (sente-transit/->TransitPacker
   :json
   t/writer-opts
   t/reader-opts))

(defn valid-handshake?
  [[k]]
  (= :chsk/handshake k))

(defn sente-pack
  [msg]
  (try
    (#'sente/pack transit-packer {} [:websocket/default msg] (uuid-str 6))
    (catch Throwable t
      (error t msg))))

(defn sente-unpack
  [s]
  (try
    (let [resp (#'sente/unpack transit-packer s)]
      (if (vector? resp)
        (first resp)
        resp))
    (catch Throwable t
      (error t s))))


;; * Events

(def ignored-events #{:chsk/ping})

(defn poll-error
  [conn-chan]
  (when-let [[id :as ev] (a/poll! conn-chan)]
    (if (= :store.sync.protocol.response/error ev)
      ev
      (println "Ignoring event" ev))))


;; * Client

(def default-buffer-size 1024)

(defn ws-chan-step-fn
  [{:keys [ws-url headers user-id] :as ctx}]
  {:pre [ws-url headers]}
  (a/go
    (try
      (debug "WS" ctx)
      (let [err         (fn [e] (do (error e) nil))
            url         (format "%s?client-id=%s" ws-url user-id)
            read-chan   (a/chan default-buffer-size
                                (comp
                                 (map sente-unpack)
                                 (remove (fn [[id]] (ignored-events id)))) err)
            write-chan  (a/chan default-buffer-size (map sente-pack) err)
            client-chan (ws/new-client-chan url {:headers headers} read-chan write-chan)
            conn-chan   (a/<! client-chan)
            handshake   (a/<! conn-chan)]
        (debug "Conn:" conn-chan handshake )
        [(valid-handshake? handshake) (assoc ctx :ws-chan conn-chan)])
      (catch Throwable t
        (error t)))))


;; * Steps

(def ws-chan-step
  {:name    "Assoc :ws-chan in the ctx map"
   :request ws-chan-step-fn})


