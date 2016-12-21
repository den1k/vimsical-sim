(ns vimsical.sim.util.manifold
  (:require
   [clojure.core.async :as a]
   [manifold.deferred :as d]
   [manifold.stream :as s]))

;; * Manifold -> core.async

(defn deferred->chan [d]
  (let [source (d/chain' d (fn [v] (if (nil? v) ::nil v)))
        sink   (a/chan 1 (remove (partial identical? ::nil)))]
    (s/connect source sink {:upstream? true :downstream? true})
    sink))

(comment
  (do
    (a/<!! (a/into [] (deferred->chan (d/future (Thread/sleep 100)))))
    (assert (= 1 (a/<!! (deferred->chan (d/future (Thread/sleep 100) 1)))))
    (a/go (assert (= 1 (a/<! (deferred->chan (d/future (Thread/sleep 100) 1))))))
    (a/go (assert (= nil (a/<! (deferred->chan (d/future (Thread/sleep 100)))))))))
