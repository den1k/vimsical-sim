(ns vimsical.sim.util.transit
  (:require
   [om.tempid]
   [om.transit]
   [cognitect.transit :as t]
   [taoensso.timbre :refer [error]])
  (:import
   clojure.lang.PersistentTreeMap
   (com.cognitect.transit ReadHandler WriteHandler)
   (java.io ByteArrayInputStream ByteArrayOutputStream)
   om.tempid.TempId
   om.transit.TempIdHandler))

(def reader-opts
  {:handlers
   {"sorted-map" (reify
                   ReadHandler
                   (fromRep [_ x] (into (sorted-map) x)))
    "om/id"      (reify
                   ReadHandler
                   (fromRep [_ id] (TempId. id)))}})

(def writer-opts
  {:handlers
   {PersistentTreeMap (reify
                        WriteHandler
                        (tag [_ _] "sorted-map")
                        (rep [_ x] (into {} x)))
    TempId            (TempIdHandler.)}})


(defn transit-write-string
  [object]
  (try
    (let [baos   (ByteArrayOutputStream.)
          writer (t/writer baos :json writer-opts)
          _      (t/write writer object)
          ret    (.toString baos)]
      (.reset baos)
      ret)
    (catch Throwable t
      (error t object "Transit encoding error"))))

(defn transit-read-string
  [^String s]
  (try
    (clojure.pprint/pprint {"READING" s})
    (let [bs (.getBytes s)
          in (ByteArrayInputStream. bs)]
      (t/read
       (t/reader in :json reader-opts)))
    (catch Throwable t
      (error t s "Transit decoding error"))))

(comment
  (let [o {:a (into (sorted-map) {1 1 2 2})}]
    (-> o
        (transit-write-string)
        (transit-read-string)
        (= o))))
