(ns codepen.feeds
  (:require
   [codepen.pipeline :as |>]
   [clojure.core.async :as a]
   [org.httpkit.client :as http]
   [cheshire.core :as json]))

(def pens-per-response 12)

(def picks-url   "http://cpv2api.com/pens/picks")
(def recent-url  "http://cpv2api.com/pens/recent")
(def popular-url "http://cpv2api.com/pens/popular")

(defn body->pens
  [body]
  (some-> body (json/parse-string true) :data not-empty))

(defn feed-chan
  [buf url]
  (let [out (a/chan buf)]
    (a/thread
      (loop [page      1
             prev-pens nil]
        (let [query          {:url url :query-params {"page" page}}
              {:keys [body]} @(http/request query)
              pens           (body->pens body)]
          (cond
            (nil? pens)
            (throw (ex-info "No pens found" {:feed url :page page}))

            (= pens prev-pens)
            (a/close! out)

            :else
            (do
              (a/<!! (a/onto-chan out pens false))
              (recur (inc page) pens))))))
    out))
