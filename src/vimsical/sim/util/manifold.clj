(ns vimsical.sim.util.manifold
  (:require
   [clojure.core.async :as a]
   [clojure.core.async.impl.protocols :as p]
   [manifold.deferred :as d]))

;; * Manifold -> core.async

(defn deferred->chan [d]
  (reify p/ReadPort
    (take! [_ fn1-handler]
      (if (realized? d)
        d
        (d/on-realized d fn1-handler fn1-handler)))))

(comment
  (assert (= 1 (a/<!! (deferred->chan (d/future 1))))))
