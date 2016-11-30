(ns vimsical.sim.util.transit
  (:require
   [om.tempid]
   [om.transit]
   [cognitect.transit :as t])
  (:import
   clojure.lang.PersistentTreeMap
   (com.cognitect.transit ReadHandler WriteHandler)
   (om.tempid TempId)
   (java.io ByteArrayInputStream ByteArrayOutputStream)
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
  (let [baos   (ByteArrayOutputStream.)
        writer (t/writer baos :json writer-opts)
        _      (t/write writer object)
        ret    (.toString baos)]
    (.reset baos)
    ret))

(defn transit-read-string
  [s]
  (t/read
   (t/reader s :json reader-opts)))


(comment
  (let [o {:a (into (sorted-map) {1 1 2 2})}]
    (-> o
        (transit-write-string)
        .getBytes
        (ByteArrayInputStream.)
        (transit-read-string)
        (= o))))
