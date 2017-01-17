(ns vimsical.sim.util.uuid
  (:import (java.util UUID)))

(defn uuid? [x] (instance? java.util.UUID x))

(defn uuid [] (UUID/randomUUID))
