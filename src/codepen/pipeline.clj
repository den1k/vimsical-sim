(ns codepen.pipeline
  (:require
   [clojure.core.async :as a]))


;; * Error handling

(defn error? [x] (or (nil? x) (= :error x)))

(defn wrap-error
  [arg]
  (if (error? arg)
    :error
    arg))

(defn wrap-error-xf
  [xf]
  (comp xf (map wrap-error)))

(defn wrap-error-af
  [af]
  (fn [arg out]
    (if (error? arg)
      (doto out
        (a/offer! :error)
        (a/close!))
      (or (af arg out) :error))))


;; * Pipelines

(defn !
  [chan n af buf]
  (let [out (a/chan buf)]
    (a/pipeline-async n out (wrap-error-af af) chan)
    out))

(defn !!
  [chan n xf buf]
  (let [out (a/chan buf)]
    (a/pipeline-blocking n out (wrap-error-xf xf) chan)
    out))

(defn sink
  ([chan] (sink chan identity))
  ([chan f]
   (a/go-loop []
     (when-let [v (a/<! chan)]
       (try (when f (f v)) (catch Throwable _))
       (recur)))))
