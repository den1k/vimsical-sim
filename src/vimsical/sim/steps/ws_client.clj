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
    (if (vector? resp)
      (first resp)
      resp)))


;; * Events

(def ignored-events #{:chsk/ws-ping})
(def error-events #{:vims/new-session})

(defn event-matches?
  [e events]
  (debug "WS event:" e)
  (cond
    (keyword? e) (contains? events e)
    (vector? e)  (event-matches? (first e) events)
    :else        (assert false)))

(defn poll-error
  [conn-chan]
  (when-let [e (a/poll! conn-chan)]
    (if (event-matches? e error-events)
      e
      (debug "Ignoring event" e))))


;; * Client

(def default-buffer-size 1024)

(defn log-xf
  [txt]
  (map (fn [e] (do (debug txt e) e))))

(def ignore-events-xf (remove (fn [e] (event-matches? e ignored-events))))
(def read-xf (comp (map sente-unpack) ignore-events-xf))
(def write-xf (map sente-pack))

(defn ws-conn-step-fn
  [{:keys [ws-url headers user-id] :as ctx}]
  {:pre [ws-url headers]}
  (a/go
    (try
      (debug "WS" ctx)
      (let [err         (fn [e] (error e))
            client-id   (uuid-str)
            url         (format "%s?client-id=%s" ws-url client-id)
            read-chan   (a/chan default-buffer-size read-xf err)
            write-chan  (a/chan default-buffer-size write-xf err)
            client-chan (ws/new-client-chan url {:headers headers} read-chan write-chan)
            conn-chan   (a/<! client-chan)
            handshake   (a/<! conn-chan)]
        (when-not (valid-handshake? handshake)
          (error "invalid handshake" handshake))
        [(valid-handshake? handshake) (assoc ctx :ws-chan conn-chan :ws-client client-chan)])
      (catch Throwable t
        (error t)))))

(defn ws-cleanup-ctx
  [ctx]
  (-> ctx
      (update :ws-chan a/close!)
      (update :ws-client a/close!)))

;; * Steps

(def ws-conn-step
  {:name    "Assoc :ws-chan in the ctx map"
   :request ws-conn-step-fn})


(def ws-cleanup-step
  {:name "Cleanup ws conn and client"
   :request ws-cleanup-ctx})
