(ns codepen.pens
  (:require
   [clojure.core.async :as a]
   [clojure.spec :as s]
   [org.httpkit.client :as http]
   [clojure.string :as str]))


;; * Download

(defn body
  [{:keys [body error] :as resp}]
  (when-not (or error (str/blank? body)) body))

(defn resource-url
  [resource {:keys [link] :as pen}]
  (str link "." (name resource)))

(defn new-pen-file-af
  [resource]
  (fn [{:keys [link] :as pen} out]
    (http/request
     {:url (resource-url resource pen) :method :get :headers {"Accept" "text/plain"}}
     (fn [resp]
       (a/go
         (try
           (if-let [data (body resp)]
             (a/>! out (assoc-in pen [:code resource] data))
             (a/>! out pen))
           (finally
             (a/close! out))))))))
