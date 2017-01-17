(ns vimsical.sim.steps.changes
  (:require
   [clojure.core.async :as a]
   [taoensso.timbre :refer [debug error trace]]
   [vimsical.sim.steps.ws-client :as ws-client]
   [vimsical.sim.util.rand :as rand]
   [vimsical.sim.util.uuid :as uuid]))


;; * Delta helpers

(defn- prev-delta-id
  [^long delta-id]
  (when (and delta-id (pos? delta-id))
    (dec delta-id)))

(defn- char->str [c] (when c (str c)))

(defn- zip-delta-ids
  [tick-fn start-delta-id string]
  (letfn [(ticks [] (let [t (long (tick-fn))] [t (inc t)]))]
    (let [deltas-count (count string)
          len          (* 2 deltas-count)
          indexes      (range start-delta-id (+ start-delta-id len))
          strs         (map char->str (interleave string  (repeat nil)))
          times        (take len (mapcat identity (repeatedly ticks)))]
      (map vector indexes strs times))))


;; * Codepen -> deltas

(defn- pen->detlas
  [rng {:keys [code]}]
  (let [sub-type {:html :html :css :css :js :javascript}]
    (loop [[pen-sub-type :as pen-sub-types] [:html :css :js]
           current-delta-id                 0
           time-acc                         0
           deltas-acc                       []]
      (if-not (seq pen-sub-types)
        deltas-acc
        (let [string                (get code pen-sub-type)
              delta-ids             (zip-delta-ids (partial rand/tick rng) current-delta-id string)
              sub-type              (get sub-type pen-sub-type)
              {:keys [deltas time]} (reduce
                                     (fn [{:keys [time deltas]} [id chr pad]]
                                       (let [prev-id       (prev-delta-id id)
                                             change-amount (count chr)
                                             op            (if (pos? change-amount)
                                                             [:str/ins prev-id (str chr)]
                                                             [:crsr/mv prev-id])]
                                         {:time   (+ (long time) (long pad))
                                          :deltas (conj
                                                   deltas
                                                   {:id            id
                                                    :prev-id       prev-id
                                                    ;; Edits in a new file don't
                                                    ;; have a :prev-same-id
                                                    :prev-same-id  (when (seq deltas) prev-id)
                                                    :op            op
                                                    :pad           pad
                                                    :file/sub-type sub-type
                                                    :meta          {:timestamp time, :version 1.0}})}))
                                     {:time 0 :deltas []} delta-ids)]
          (recur
           (next pen-sub-types)
           (or (some-> delta-ids last first inc) current-delta-id)
           (+ (long time-acc) (long time))
           (into deltas-acc deltas)))))))

(defn find-file-by-sub-type
  [target-sub-type files]
  {:post [%]}
  (some
   (fn [{:keys [file/sub-type] :as file}] (when (= target-sub-type sub-type) file))
   files))

(defn- deltas->tx
  [deltas token branch-uuid files]
  {:pre [(some? token) (uuid/uuid? branch-uuid) (seq files)]}
  [(list
    'vims/add-deltas
    {:store.sync.protocol/token token
     :store.sync.protocol/deltas
     (mapv
      (fn update-delta-with-file-and-branch
        [{:keys [file/sub-type] :as delta}]
        (let [{:keys [file/uuid]} (find-file-by-sub-type sub-type files)]
          (-> delta
              (dissoc :file/sub-type)
              (assoc :file-uuid uuid :branch-uuid branch-uuid))))
      deltas)})])

(defn- deltas-time ^long
  [deltas]
  (reduce + (map :pad deltas)))

(defn- partition-deltas
  ([deltas] (partition-all 2 deltas))
  ([max-batch-interval-ms max-batch-count deltas]
   {:post [(= deltas (apply concat %))]}
   (let [batch (volatile! 0)
         t     (volatile! 0)
         size  (volatile! 0)]
     (partition-by
      (fn [{:keys [pad] :as delta}]
        (if-not (or (>= (vswap! t unchecked-add pad) max-batch-interval-ms)
                    (>= (vswap! size unchecked-inc) max-batch-count))
          @batch
          (do (vreset! size 0)
              (vreset! t 0)
              (vswap! batch inc))))
      deltas))))


;; * Step

(defn- new-add-deltas-fn
  [deltas]
  (fn add-change-fn
    [{:as   ctx
      :keys [ws-chan rng token branch-uuid files]}]
    {:pre [ws-chan rng branch-uuid (seq files)]}
    (a/go
      (try
        (debug "Step" ctx)
        (if-some [err (ws-client/poll-error ws-chan)]
          ;; No retry strategy yet...
          (error "error" err)
          (let [tx     (deltas->tx deltas token branch-uuid files)
                _ (debug tx)
                _offer (a/offer! ws-chan tx)]
            (when-not _offer
              (a/close! ws-chan)
              (error "offer failed"))
            _offer))
        (catch Throwable t
          (a/close! ws-chan)
          (error t))))))

(defn new-pen-changes-steps
  [rng batch-interval-ms batch-max-count {:keys [id] :as pen}]
  ;; NOTE use a random delay so all clients don't start typing at the same time...
  (let [init-sleep (* (double batch-interval-ms) (rand/gaussian rng))]
    (->> pen
         (pen->detlas rng)
         (partition-deltas batch-interval-ms batch-max-count)
         (reduce
          (fn [{:keys [sleep steps] :as acc} deltas]
            {:pre [(number? sleep)]}
            {:sleep (deltas-time deltas)
             :steps (conj steps
                          {:name         "add-deltas"
                           :request      (new-add-deltas-fn deltas)
                           :sleep-before (constantly sleep)})})
          {:sleep init-sleep :steps []})
         :steps)))

(comment
  (let [deltas (pen->detlas
                (java.util.Random.)
                (first (vimsical.sim.core/load-pens-from-dir vimsical.sim.core/pens-default-dir 1)))
        batches (partition-deltas 1000 100 deltas)]
    (dorun
     (for [batch batches
           delta batch]
       (println (:id delta))))))
