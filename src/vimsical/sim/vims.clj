(ns vimsical.sim.vims
  (:require
   [om.next :as om]
   [vimsical.sim.queries :as q]
   [vimsical.sim.util.http :as http]
   [vimsical.sim.util.ws :as ws]
   [clojure.core.async :as a])
  (:import
   (java.util Random)))


;; * Step fns

;; ** User query

(defn user-step
  "Get the user (query when loading the page)"
  [{:keys [remote-url] :as ctx}]
  {:pre [remote-url]}
  (a/go
    (let [req          {:request-method :post
                        :url            remote-url
                        :body           q/app-user}
          resp         (a/<! (http/req-chan req))
          ring-session (get-in resp [:headers "set-cookie"])]
      [true (assoc-in ctx [:headers "Cookie"] ring-session)])))

;; ** WS init

(defn ws-chan-step
  "Assoc the ws-chan in the ctx map"
  [{:keys [ws-url headers] :as ctx}]
  {:pre [ws-url headers]}
  (a/go
    (let [read-chan   (a/chan)
          write-chan  (a/chan)
          client-chan (ws/new-client-chan ws-url {:headers headers} read-chan write-chan)
          conn-chan   (a/<! client-chan)]
      [true (assoc ctx :ws-chan conn-chan)])))


;; ** New vims

(defn valid-new-vims-resp? [resp] true)

(defn merge-vims-ctx
  [ctx {:keys [body] :as resp}]
  {:pre  [body]
   :post [(some? (:app-user-id %))
          (seq (:vims %))
          (some? (:vims-id %))
          (some? (:branch %))
          (some? (:branch-id %))
          (seq (:stores %))]}
  (let [{:keys [app/user]}                 body
        {:keys [user/vimsae]}              user
        {:keys [vims/branches] :as vims}   (last vimsae)
        {:keys [branch/stores] :as branch} (first branches)]
    (merge ctx
           {:app-user-id (:db/id user)
            :vims        vims
            :vims-id     (:db/id vims)
            :branch      branch
            :branch-id   (:db/id branch)
            :stores      stores})))

(defn new-vims-step
  "Create a new vims"
  [{:keys [remote-url headers] :as ctx}]
  {:pre [remote-url headers]}
  (a/go
    (let [req  {:headers        headers
                :request-method :post
                :url            remote-url
                :body           (q/vims-new (om/tempid))}
          resp (a/<! (http/req-chan req))]
      (and
       (valid-new-vims-resp? resp)
       [true (merge-vims-ctx ctx resp)]))))

;; ** Add changes

(defn pen-change
  [prev-delta-id stores code-type index pen]
  (let [string    (get-in pen [:code code-type])
        character (nth string index)
        insert-deltas
        (if prev-delta-id
          {(inc (long prev-delta-id)) [prev-delta-id [:string/insert [prev-delta-id (str character)]]]}
          {0 [nil ""]
           1 [0 [:string/insert [0 (str character)]]]})
        move-deltas
        (if prev-delta-id
          {(inc (inc (long prev-delta-id))) [(inc (long prev-delta-id)) [:cursor/move [1]]]}
          {2 [1 [:cursor/move [1]]]})]))

(defn prev-delta-id
  [^long delta-id]
  (when (and delta-id (pos? delta-id))
    (dec delta-id)))

(defn skew-rand
  "min - the minimum skewed value possible
   max - the maximum skewed value possible
   skew - the degree to which the values cluster around the mode of the distribution; higher values mean tighter clustering
   bias - the tendency of the mode to approach the min, max or midpoint value; positive values bias toward max, negative values toward min"
  ^Double [^Random rng min max skew bias]
  (let [r           (- (long max) (long min))
        mid         (+ (long min) (/ r 2.0))
        unit        (.nextGaussian rng)
        bias-factor (Math/exp (long bias))]
    ;; double retval = mid+(range*(bias-factor/(biasFactor+Math.exp(-unitGaussian/skew))-0.5));
    (double
     (+ mid
        (* r
           (- (/ bias-factor
                 (+ bias-factor (Math/exp (- (/ unit (double skew))))))
              0.5))))))

(defn tick
  ([] (tick (Random.)))
  ([^Random rng] (tick rng 3 2))
  ([^Random rng skew bias]
   (let [min-cpm 20 max-cpm 500]
     (long (* 100 (double (/ 60 (double (skew-rand rng min-cpm max-cpm skew bias)))))))))

(defn new-mutable-tick
  [^Random rng]
  (let [total (atom nil)]
    (fn mutable-tick []
      (if (nil? (deref total))
        (reset! total 0)
        (swap! total + (tick rng))))))

(comment
  (defn type-string
    ([s] (type-string (Random.) s))
    ([^Random rng s]
     (let [t (new-mutable-tick rng)]
       (a/go-loop [[c & chars] s]
         (when c
           (a/<! (a/timeout (t)))
           (print c)
           (flush)
           (recur chars))))))
  (type-string "asdfasdfasdfasdfasdf"))

(defn char->str [c] (when c (str c)))

(defn zip-delta-ids
  [tick string]
  (letfn [(ticks [] (let [t (tick)] [t t]))]
    (let [deltas-count (count string)
          len          (* 2 deltas-count)
          indexes      (range len)
          strs         (map char->str (into [""] (interleave string  (repeat nil))))
          times        (take len (mapcat identity (repeatedly ticks)))]
      (map vector indexes strs times))))

(comment
  (zip-delta-ids (new-mutable-tick (Random.)) "abcd")
  '([0 "" 30]                           ; stub
    [1 "a" 30]                          ; first change
    [2 nil 55]                          ; first move
    [3 "b" 55]                          ; etc...
    [4 nil 82]
    [5 "c" 82]
    [6 nil 106]
    [7 "d" 106]))

(defn pen->detlas-and-timelines
  "Given a pen returns a seq of maps with values for :deltas and :timeline
  representing the arguments to add change for the given pen.}"
  [^Random rng {:keys [code]}]
  (for [code-type [:html :css :js]
        :let [string (get code code-type)]
        [index character t] (zip-delta-ids (new-mutable-tick rng) string)
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

(defn code-type->content-type
  [code-key]
  (get {:css "text/css" :html "text/html" :js "text/javascript"} code-key))

(defn find-store
  [code-type stores]
  (let [content-type (code-type->content-type code-type)]
    (some
     (fn [{:keys [store/file] :as store}]
       (and (= content-type (:file/content-type file)) store))
     stores)))

(defn add-changes-mutations
  [^Random rng pen app-user-id vims-id branch-id stores]
  (for [{:keys [deltas timeline type] :as dts} (pen->detlas-and-timelines rng pen)]
    (let [{{file-id :db/id} :store/file store-id :db/id} (find-store type stores)]
      (list
       'vims/add-change
       {{:user_id   app-user-id
         :vims_id   vims-id
         :branch_id branch-id
         :file_id   file-id
         :store_id  store-id}
        {:deltas deltas :timeline timeline}}))))

(defn tx-max-time [txs]
  (->> (for [[_ m] txs] (keys (:timeline (first (vals m)))))
       (apply concat)
       (apply max)))

(defn valid-add-changes-resp? [resp] true)

(defn new-add-changes-step
  [{:keys [code] :as pen}]
  (fn [{:as ctx
        :keys
        [ws-chan
         rng
         headers
         app-user-id vims-id branch-id
         stores]}]
    (a/go-loop
        [t           0
         [tx & more] (partition-all 2 (add-changes-mutations rng pen app-user-id vims-id branch-id stores))]
      (or
       (nil? tx)
       (let [t'    (tx-max-time tx)
             dt    (- (long t') (long t))
             _wait (a/<! (a/timeout dt))
             _put  (a/>! ws-chan tx)
             resp  (a/<! ws-chan)]
         ;; TODO check resp
         (println tx resp)
         (and
          (valid-add-changes-resp? resp)
          (recur (long t') more)))))))

#_
(let [step (new-add-changes-step test-pen)]
  (step {:rng         (Random.)
         :app-user-id :user-id
         :vims-id     :vims-id
         :branch-id   :branch-id
         :stores      [{:db/id 1 :store/file {:db/id 2 :file/content-type "text/html"}}
                       {:db/id 3 :store/file {:db/id 4 :file/content-type "text/css"}}
                       {:db/id 5 :store/file {:db/id 6 :file/content-type "text/javascript"}}]}))




(tx-max-time
 '((vims/add-change
    {{:user_id :user-id,
      :vims_id :vims-id,
      :branch_id :branch-id,
      :file_id nil,
      :store_id nil}
     {:deltas nil, :timeline {10 foo}}})
   (vims/add-change
    {{:user_id :user-id,
      :vims_id :vims-id,
      :branch_id :branch-id,
      :file_id nil,
      :store_id nil}
     {:deltas nil, :timeline {1 bar}}})))
