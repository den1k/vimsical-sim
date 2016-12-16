(ns vimsical.sim.steps.changes
  (:require
   [vimsical.sim.steps.ws-client :as ws-client]
   [taoensso.timbre :refer [debug error]]
   [vimsical.sim.util.rand :as rand]
   [clojure.core.async :as a]))

;; * Helpers
;; ** Delta graph

(defn- prev-delta-id
  [^long delta-id]
  (when (and delta-id (pos? delta-id))
    (dec delta-id)))

(defn- char->str [c] (when c (str c)))

(defn unique-timestamps?
  [delta-ids]
  (let [ts (map #(nth % 2) delta-ids)]
    (= ts (distinct ts))))

(defn- zip-delta-ids
  [tick-fn string]
  {:post [(unique-timestamps? %)]}
  (letfn [(ticks [] (let [t (long (tick-fn))] [t (inc t)]))]
    (let [deltas-count (count string)
          len          (* 2 deltas-count)
          indexes      (range len)
          strs         (map char->str (into [""] (interleave string  (repeat nil))))
          times        (take len (mapcat identity (repeatedly ticks)))]
      (map vector indexes strs times))))

(comment
  (zip-delta-ids (rand/new-mutable-tick (rand/rng)) "abcd")
  ([0 "" 0]
   [1 "a" 1]
   [2 nil 13]
   [3 "b" 14]
   [4 nil 26]
   [5 "c" 27]
   [6 nil 39]
   [7 "d" 40]))

(defn- pen->detlas-and-timelines
  "Given a pen returns a seq of maps with values for :deltas and :timeline
  representing the arguments to add change for the given pen.}"
  [rng {:keys [code]}]
  (for [code-type           [:html :css :js]
        :let                [string (get code code-type)]
        [index character t] (zip-delta-ids (rand/new-mutable-tick rng) string)
        :when               (pos? (count string))]
    (let [prev-id       (prev-delta-id index)
          change-amount (count character)
          change-type   (cond
                          (pos? change-amount) :string/insert
                          (zero? (long index)) :branch/new
                          :else                :cursor/move)
          change-meta   (case change-type :cursor/move [prev-id] [prev-id character])
          change        (if prev-id [change-type change-meta] "")]
      {:type   code-type
       :deltas {index [prev-id change]}
       :timeline
       (into
        (sorted-map)
        {t #{{:delta-id      index
              :change-type   change-type
              :change-amount change-amount}}})})))

(comment
  (pen->detlas-and-timelines (java.util.Random.) {:code {:js "var foo = bar;"}}))

;; ** Pens

(defn- code-type->content-type
  [code-key]
  (get {:css "text/css" :html "text/html" :js "text/javascript"} code-key))

(defn- find-store
  [code-type stores]
  {:post [(-> % :db/id) (-> % :store/file :db/id)]}
  (let [content-type (code-type->content-type code-type)]
    (some
     (fn [{:keys [store/file] :as store}]
       (and (= content-type (:file/content-type file)) store))
     stores)))

(defn- changes->tx
  [changes token app-user-id vims-id branch-id stores]
  {:pre [(number? app-user-id)
         (number? vims-id)
         (number? branch-id)
         (some? token)]}
  (mapv
   (fn [{:keys [deltas timeline type] :as dts}]
     (let [{{file-id :db/id} :store/file store-id :db/id} (find-store type stores)]
       (list
        'vims/add-change
        {:store.sync.protocol/token token
         :store.sync.protocol/changes
         {{:user_id   app-user-id
           :vims_id   vims-id
           :branch_id branch-id
           :file_id   file-id
           :store_id  store-id}
          {:deltas deltas :timeline timeline}}})))
   changes))

(defn- tx-max-time [txs]
  (->> (for [[_ m] txs]
         (keys (:timeline (first (vals (:store.sync.protocol/changes m))))))
       (apply concat)
       (apply max)))

;; * Step

(defn partition-changes
  [mutations]
  ;; Need to send 3 mutations for the first one because of the stub
  (let [head (vec (take 3 mutations))
        tail (drop 3 mutations)]
    (into [head] (comp
                  (partition-all 2)
                  (map vec))
          tail)))

(defn new-add-change-fn
  [changes]
  (fn add-change-fn
    [{:as ctx
      :keys [t
             ws-chan
             rng
             token app-user-id vims-id branch-id
             stores]
      :or {t -1}}]
    {:pre [ws-chan rng app-user-id vims-id branch-id (seq stores)]}
    (cond
      ;; No retry strategy yet
      (ws-client/poll-error ws-chan)
      (do (debug "error") false)

      :else
      (a/go
        (try
          (let [tx    (changes->tx changes token app-user-id vims-id branch-id stores)
                t'    (tx-max-time tx)
                dt    (- (long t') (long t))
                _wait (a/<! (a/timeout dt))
                _put  (a/>! ws-chan tx)]
            (debug "Tx" tx)
            [true (assoc ctx :t t')])
          (catch Throwable t
            (error t)))))))

(defn new-pen-changes-steps
  [rng {:keys [id] :as pen}]
  (->> pen
       (pen->detlas-and-timelines rng)
       (partition-changes)
       (map-indexed (fn [i changes]
                      {:name    (str "add-change" id "-" i)
                       :request (new-add-change-fn changes)}))))

(comment
  (partition-changes
   (changes->tx
    (pen->detlas-and-timelines
     (java.util.Random.)
     {:images {:small "http://codepen.io/jtangelder/pen/ABFnd/image/small.png", :large "http://codepen.io/jtangelder/pen/ABFnd/image/large.png"}, :comments "1", :loves "11", :title "Vertical Pan Hammer.js example", :details "", :link "http://codepen.io/jtangelder/pen/ABFnd", :id "ABFnd", :code {:html "<script src=\"https://hammerjs.github.io/dist/hammer.js\"></script>\n\n<div id=\"myElement\"></div>", :css "#myElement {\n  background: silver;\n  height: 300px;\n  text-align: center;\n  font: 30px/300px Helvetica, Arial, sans-serif;\n}\n", :js "var myElement = document.getElementById('myElement');\n\n// create a simple instance\n// by default, it only adds horizontal recognizers\nvar mc = new Hammer(myElement);\n\n// let the pan gesture support all directions.\n// this will block the vertical scrolling on a touch-device while on the element\nmc.get('pan').set({ direction: Hammer.DIRECTION_ALL });\n\n// listen to events...\nmc.on(\"panleft panright panup pandown tap press\", function(ev) {\n    myElement.textContent = ev.type +\" gesture detected.\";\n});"}, :user {:nicename "Jorik Tangelder", :username "jtangelder", :avatar "https://gravatar.com/avatar/dd965ce7f185044f157d255cf3e65662?s=80&d=https://codepen.io/assets/avatars/user-avatar-80x80-fd2a2ade7f141e06f8fd94c000d6ac7a.png"}, :views "77196"})
    "foo" 1 2 3
    [{:db/id 4 :store/file {:db/id 5 :file/content-type "text/css"}}
     {:db/id 6 :store/file {:db/id 7 :file/content-type "text/html"}}
     {:db/id 8 :store/file {:db/id 9 :file/content-type "text/javascript"}}]
    )))


{{:vims_id 17592186045872, :branch_id 17592186045873, :user_id 17592186045871, :file_id 17592186045878, :store_id 17592186045874}
 {:timeline {0 #{{:delta-id 1, :change-amount 1, :change-type :string/insert}},
             13 #{{:delta-id 2, :change-amount 0, :change-type :cursor/move}}},
  :deltas {0 [nil ""],
           1 [0 [:string/insert [0 "<"]]],
           2 [1 [:cursor/move [1]]]}}}
