(ns codepen.fs
  (:require
   [me.raynes.fs :as fs]
   [clojure.edn :as edn]))

(defn mkdirs!
  [f]
  (or (fs/mkdirs f) true))

(defn pen-writer
  [base-dir]
  {:pre [(mkdirs! base-dir)]}
  (fn [{:keys [id] :as pen}]
    (let [edn-file (fs/file base-dir (str id ".edn"))]
      (do
        (spit edn-file (str pen))
        pen))))
