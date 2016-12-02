(ns vimsical.sim.steps.changes
  (:require
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

(defn- zip-delta-ids
  [tick-fn string]
  (letfn [(ticks [] (let [t (tick-fn)] [t t]))]
    (let [deltas-count (count string)
          len          (* 2 deltas-count)
          indexes      (range len)
          strs         (map char->str (into [""] (interleave string  (repeat nil))))
          times        (take len (mapcat identity (repeatedly ticks)))]
      (map vector indexes strs times))))

(comment
  (zip-delta-ids (rand/new-mutable-tick (rand/rng)) "abcd")
  '([0 "" 30]                           ; stub
    [1 "a" 30]                          ; first change
    [2 nil 55]                          ; first move
    [3 "b" 55]                          ; etc...
    [4 nil 82]
    [5 "c" 82]
    [6 nil 106]
    [7 "d" 106]))

(defn- pen->detlas-and-timelines
  "Given a pen returns a seq of maps with values for :deltas and :timeline
  representing the arguments to add change for the given pen.}"
  [rng {:keys [code]}]
  (for [code-type [:html :css :js]
        :let [string (get code code-type)]
        [index character t] (zip-delta-ids (rand/new-mutable-tick rng) string)
        :when (pos? (count string))]
    (let [prev-id       (prev-delta-id index)
          change-amount (count character)
          change-type   (cond
                          (pos? change-amount) :string/insert
                          (zero? index)        :branch/new
                          :else                :cursor/move)
          change        (if prev-id [change-type [prev-id character]] "")]
      {:type     code-type
       :deltas   {index [prev-id change]}
       :timeline {t #{{:delta-id index :change-type change-type :change-amout change-amount}}}})))

(comment
  (pen->detlas-and-timelines (Random.) {:code {:js "var foo = bar;"}}))

;; ** Pens

(defn- code-type->content-type
  [code-key]
  (get {:css "text/css" :html "text/html" :js "text/javascript"} code-key))

(defn- find-store
  [code-type stores]
  {:post [%]}
  (let [content-type (code-type->content-type code-type)]
    (some
     (fn [{:keys [store/file] :as store}]
       (and (= content-type (:file/content-type file)) store))
     stores)))

(defn- add-changes-mutations
  [rng pen app-user-id vims-id branch-id stores]
  {:pre [(number? app-user-id)
         (number? vims-id)
         (number? branch-id)]}
  (for [{:keys [deltas timeline type] :as dts} (pen->detlas-and-timelines rng pen)]
    (let [{{file-id :db/id} :store/file store-id :db/id} (find-store type stores)]
      (assert (number? file-id))
      (assert (number? store-id))
      (list
       'vims/add-change
       {{:user_id   app-user-id
         :vims_id   vims-id
         :branch_id branch-id
         :file_id   file-id
         :store_id  store-id}
        {:deltas deltas :timeline timeline}}))))

(defn- tx-max-time [txs]
  (->> (for [[_ m] txs] (keys (:timeline (first (vals m)))))
       (apply concat)
       (apply max)))

;; * Step

(defn- valid-add-changes-resp? [resp]
  (= {'vims/add-change {:result '()}} resp))

(defn partition-mutations
  [mutations]
  ;; Need to send 3 mutations for the first one because of the stub
  (let [head (vec (take 3 mutations))
        tail (drop 3 mutations)]
    (into [head] (comp
                  (partition-all 2)
                  (map vec))
          tail)))

(defn changes-step-fn
  [{:as ctx
    :keys
    [ws-chan
     rng
     headers
     app-user-id vims-id branch-id
     stores]}
   {:keys [code] :as pen}]
  {:pre [ws-chan rng headers app-user-id vims-id branch-id (seq stores)]}
  (let [mutations (add-changes-mutations rng pen app-user-id vims-id branch-id stores)]
    (a/go
      (try
        (debug "Step" ctx)
        (loop
            [t 0
             [tx & more] (partition-mutations mutations)]
          (debug "Transactions" tx)
          (or
           (nil? tx)
           (let [t'    (tx-max-time tx)
                 dt    (- (long t') (long t))
                 _wait (a/<! (a/timeout dt))
                 _put  (a/>! ws-chan tx)
                 resp  (a/<! ws-chan)]
             (debug "Tx" tx)
             (debug "Resp" resp)
             (when (valid-add-changes-resp? resp)
               (recur (long t') more)))))
        (catch Throwable t
          (error t))))))

(defn new-pen-changes-step
  [{:keys [id] :as pen}]
  {:name    "Pen changes"
   :request (fn pen-changes [ctx]
              (changes-step-fn ctx pen))})

(comment
  (partition-mutations
   (add-changes-mutations
    (rand/rng)
    {:images {:small "http://codepen.io/jtangelder/pen/ABFnd/image/small.png", :large "http://codepen.io/jtangelder/pen/ABFnd/image/large.png"}, :comments "1", :loves "11", :title "Vertical Pan Hammer.js example", :details "", :link "http://codepen.io/jtangelder/pen/ABFnd", :id "ABFnd", :code {:html "<script src=\"https://hammerjs.github.io/dist/hammer.js\"></script>\n\n<div id=\"myElement\"></div>", :css "#myElement {\n  background: silver;\n  height: 300px;\n  text-align: center;\n  font: 30px/300px Helvetica, Arial, sans-serif;\n}\n", :js "var myElement = document.getElementById('myElement');\n\n// create a simple instance\n// by default, it only adds horizontal recognizers\nvar mc = new Hammer(myElement);\n\n// let the pan gesture support all directions.\n// this will block the vertical scrolling on a touch-device while on the element\nmc.get('pan').set({ direction: Hammer.DIRECTION_ALL });\n\n// listen to events...\nmc.on(\"panleft panright panup pandown tap press\", function(ev) {\n    myElement.textContent = ev.type +\" gesture detected.\";\n});"}, :user {:nicename "Jorik Tangelder", :username "jtangelder", :avatar "https://gravatar.com/avatar/dd965ce7f185044f157d255cf3e65662?s=80&d=https://codepen.io/assets/avatars/user-avatar-80x80-fd2a2ade7f141e06f8fd94c000d6ac7a.png"}, :views "77196"}

    1 2 3
    [{:db/id 4 :store/file {:db/id 5 :file/content-type "text/css"}}
     {:db/id 6 :store/file {:db/id 7 :file/content-type "text/html"}}
     {:db/id 8 :store/file {:db/id 9 :file/content-type "text/javascript"}}]
    )))
