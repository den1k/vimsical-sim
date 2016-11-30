(ns codepen.collections
  (:require
   [codepen.pipeline :as |>]
   [clojure.core.async :as a]
   [org.httpkit.client :as http]
   [cheshire.core :as json]))

(def collections-per-page 6)

(def picks-url "http://cpv2api.com/collections/picks")
(def popular-url "http://cpv2api.com/collections/popular")

(defn parse-body
  [body]
  (some-> body (json/parse-string true) :data not-empty))

(defn collections-chan
  [buf url]
  (let [out (a/chan buf)]
    (a/thread
      (loop [page       1
             prev-colls nil]
        (let [query          {:url url :query-params {"page" page}}
              {:keys [body]} @(http/request query)
              colls           (parse-body body)]
          (if (= colls prev-colls)
            (a/close! out)
            (do
              (a/<!! (a/onto-chan out colls false))
              (recur (inc page) colls))))))
    out))

(defn collection-url
  [{:keys [id] :as collection}]
  (str "http://cpv2api.com/collection/" id))

(defn collection-af
  [collection out]
  (http/request
   {:url (collection-url collection)}
   (fn [{:keys [body] :as response}]
     (a/go
       (try
         (a/<! (a/onto-chan out (parse-body body)))
         (catch Throwable _)
         (finally
           (a/close! out)))))))


(defn collection-chan
  [buf n url]
  (let [collections-buf (/ buf collections-per-page)]
    (-> (collections-chan collections-buf  url)
        (|>/! n collection-af buf))))
