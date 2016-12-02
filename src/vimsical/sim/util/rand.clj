(ns vimsical.sim.util.rand
  (:import
   (java.util Random)))

;; * RNG

(defn rng ^Random
  ([] (rng nil))
  ([seed] (Random. (or seed (System/currentTimeMillis)))))

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

;; * Ticks

(defn tick
  ([] (tick (Random.)))
  ([^Random rng] (tick rng 3 2))
  ([^Random rng skew bias]
   (let [min-cpm 20
         max-cpm 500]
     (long (* 100 (double (/ 60 (double (skew-rand rng min-cpm max-cpm skew bias)))))))))

(defn new-mutable-tick
  [^Random rng]
  (let [total (atom nil)]
    (fn mutable-tick []
      (if (nil? (deref total))
        (reset! total 0)
        (swap! total + (tick rng))))))

(comment
  (do
    (require '[clojure.core.async :as a])
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
    (type-string "asdfasdfasdfasdfasdf")))
