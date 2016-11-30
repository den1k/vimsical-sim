(ns codepen.core
  (:require
   [clojure.core.async :as a]
   [clojure.set :as set]
   [codepen
    [collections :as colls]
    [feeds :as feeds]
    [fs :as fs]
    [pens :as pens]
    [pipeline :as =]]))

(defn -main
  [& out-dir]
  (let [base-dir           (or out-dir "/Users/julien/projects/codepen/resources/pens/")
        buf                (* 10 feeds/pens-per-response)
        par                16
        ;; Pen feeds
        picks-chan         (feeds/feed-chan buf feeds/picks-url)
        recent-chan        (feeds/feed-chan buf feeds/recent-url)
        popular-chan       (feeds/feed-chan buf feeds/popular-url)
        ;; Collections feeds
        picks-colls-chan   (colls/collection-chan buf par colls/picks-url)
        popular-colls-chan (colls/collection-chan buf par colls/popular-url)]
    (-> (a/merge [picks-chan
                  recent-chan
                  popular-chan
                  picks-colls-chan
                  popular-colls-chan])
        (=/! par (pens/new-pen-file-af :js) buf)
        (=/! par (pens/new-pen-file-af :css) buf)
        (=/! par (pens/new-pen-file-af :html) buf)
        (=/!! 1 (map (fs/pen-writer base-dir)) (* par buf))
        (=/sink (fn [e] (println (:id e))))
        (a/<!!))))
