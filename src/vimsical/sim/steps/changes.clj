(ns vimsical.sim.steps.changes
  (:require
   [clojure.core.async :as a]
   [taoensso.timbre :refer [debug error trace]]
   [vimsical.sim.steps.ws-client :as ws-client]
   [vimsical.sim.util.rand :as rand]))

;; * Delta graph

(defn- prev-delta-id
  [^long delta-id]
  (when (and delta-id (pos? delta-id))
    (dec delta-id)))

(defn- char->str [c] (when c (str c)))

(defn unique-timestamps?
  [delta-ids]
  (reduce
   (fn [^long prev [_ _ ^long curr]]
     (if (== prev curr)
       (reduced false)
       curr))
   -1 delta-ids))

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


;; * Codepen -> deltas

(defn- pen->detlas-and-timelines
  "Given a pen returns a seq of maps with values for :deltas and :timeline
  representing the arguments to add change for the given pen.}"
  [rng {:keys [code]}]
  (let [tick (rand/new-mutable-tick rng)]
    (for [code-type           [:html :css :js]
          :let                [string (get code code-type)]
          [index character t] (zip-delta-ids tick string)
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
                :change-amount change-amount}}})}))))

(comment
  (pen->detlas-and-timelines (java.util.Random.) {:code {:js "var foo = bar;"}}))

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
  (or (some->> (for [[_ m] txs]
                 (keys (:timeline (first (vals (:store.sync.protocol/changes m))))))
               (apply concat)
               (not-empty)
               (apply max))
      0))


;; * Step

(defn check-changes
  [{:keys [deltas timeline] :as changes}]
  (let [d (ffirst deltas)
        t (first (map :delta-id (second (first timeline))))]
    (assert d)
    (assert t)
    (assert (= d t)))
  changes)

(defn partition-changes
  ([mutations]
   {:post [(every? (fn [batch] (every? map? batch)) %)
           (mapv (fn [batch] (mapv check-changes batch)) %)]}
   ;; Need to send 3 mutations for the first one because of the stub
   (let [head (vec (take 3 mutations))
         tail (drop 3 mutations)]
     (into [head] (comp
                   (partition-all 2)
                   (map vec))
           tail)))
  ([^long max-batch-interval-ms ^long max-batch-count mutations]
   {:post [(every? (fn [batch] (every? map? batch)) %)
           (mapv (fn [batch] (mapv check-changes batch)) %)]}
   (letfn [(reduce-tail [tail]
             (reduce
              (fn [{:keys [coll batch ^long batch-begin] :as acc} {:keys [timeline deltas] :as el}]
                (let [t      (long (apply max (keys timeline)))
                      dt     (- t batch-begin)
                      batch' (conj batch el)]
                  (if (or (>= dt max-batch-interval-ms)
                          (>= (count batch') max-batch-count))
                    {:coll (conj coll batch') :batch [] :batch-begin t}
                    {:coll coll :batch batch' :batch-begin batch-begin})))
              {:coll [] :batch [] :batch-begin 0} tail))]
     (let [head                 (vec (take 3 mutations))
           tail                 (drop 3 mutations)
           {:keys [coll batch]} (reduce-tail tail)]
       (concat [head] coll [batch])))))

(defn new-add-change-fn
  [batch-interval-ms batch-max-count changes]
  (fn add-change-fn
    [{:as   ctx
      :keys [t
             ws-chan
             rng
             token app-user-id vims-id branch-id
             stores]}]
    {:pre [ws-chan rng app-user-id vims-id branch-id (seq stores)]}
    (try
      (debug "Step" ctx)
      (if-let [err (ws-client/poll-error ws-chan)]
        ;; No retry strategy yet
        (error "error" err)
        (a/go
          (try
            (let [tx     (changes->tx changes token app-user-id vims-id branch-id stores)
                  ;; NOTE use a random delay so all clients don't start typing at the same time...
                  t      (if-not (nil? t) t (* batch-interval-ms (rand/gaussian rng)))
                  t'     (tx-max-time tx)
                  dt     (- (long t') (long t))
                  _wait  (a/<! (a/timeout (max dt (- dt))))
                  _offer (a/offer! ws-chan tx)]
              (when-not _offer
                (a/close! ws-chan)
                (error "offer failed"))
              (trace "Tx delay" dt  tx)
              [_offer (assoc ctx :t t')])
            (catch Throwable t
              (error t)))))
      (catch Throwable t
        (a/close! ws-chan)
        (error t)))))

(defn new-pen-changes-steps
  [rng batch-interval-ms batch-max-count {:keys [id] :as pen}]
  (->> pen
       (pen->detlas-and-timelines rng)
       (partition-changes batch-interval-ms batch-max-count)
       (map-indexed
        (fn [i changes]
          {:name    "add-change"
           :request (new-add-change-fn batch-interval-ms batch-max-count changes)}))))


(comment
  (defn report-batches
    [changes]
    (into (sorted-map)
          (frequencies (map count changes))))

  (let [changes (pen->detlas-and-timelines (java.util.Random.) {:images {:small "http://codepen.io/jtangelder/pen/ABFnd/image/small.png", :large "http://codepen.io/jtangelder/pen/ABFnd/image/large.png"}, :comments "1", :loves "11", :title "Vertical Pan Hammer.js example", :details "", :link "http://codepen.io/jtangelder/pen/ABFnd", :id "ABFnd", :code {:html "<script src=\"https://hammerjs.github.io/dist/hammer.js\"></script>\n\n<div id=\"myElement\"></div>", :css "#myElement {\n  background: silver;\n  height: 300px;\n  text-align: center;\n  font: 30px/300px Helvetica, Arial, sans-serif;\n}\n", :js "var myElement = document.getElementById('myElement');\n\n// create a simple instance\n// by default, it only adds horizontal recognizers\nvar mc = new Hammer(myElement);\n\n// let the pan gesture support all directions.\n// this will block the vertical scrolling on a touch-device while on the element\nmc.get('pan').set({ direction: Hammer.DIRECTION_ALL });\n\n// listen to events...\nmc.on(\"panleft panright panup pandown tap press\", function(ev) {\n    myElement.textContent = ev.type +\" gesture detected.\";\n});"}, :user {:nicename "Jorik Tangelder", :username "jtangelder", :avatar "https://gravatar.com/avatar/dd965ce7f185044f157d255cf3e65662?s=80&d=https://codepen.io/assets/avatars/user-avatar-80x80-fd2a2ade7f141e06f8fd94c000d6ac7a.png"}, :views "77196"})
        batches (partition-changes 1000 1000 changes)
        txs  (map
              (fn [batch]
                (changes->tx
                 batch
                 "foo" 1 2 3
                 [{:db/id 4 :store/file {:db/id 5 :file/content-type "text/css"}}
                  {:db/id 6 :store/file {:db/id 7 :file/content-type "text/html"}}
                  {:db/id 8 :store/file {:db/id 9 :file/content-type "text/javascript"}}]))
              batches)]
    ;; (doseq [batch batches]
    (check-changes changes))
  )
