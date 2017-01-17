(ns vimsical.sim.util.rand
  (:import java.util.Random))

;; * RNG

(defn rng ^Random
  ([] (rng nil))
  ([seed] (Random. (or seed (System/nanoTime)))))

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

(defn gaussian ^double
  [^Random rng]
  (.nextGaussian rng))

(defn tick
  ([^Random rng]
   (let [skew (* 3 (gaussian rng))
         bias (* 2 (gaussian rng))]
     (tick rng skew bias)))
  ([^Random rng skew bias]
   (long (* 100 (double (/ 60 (double (skew-rand rng 100 400 skew bias))))))))


;; * Testing

(comment
  (do
    (defn sort-by-val
      [m]
      (into
       (sorted-map-by
        (fn [k1 k2]
          (let [v_c (compare (m k1) (m k2))]
            (if (= 0 v_c)
              (compare k1 k2)
              (- v_c)))))
       m))
    (let [rng (rng)]
      (sort-by-val
       (frequencies (repeatedly 100 #(tick rng 1 0)))))

    (require '[clojure.core.async :as a])
    (defn type-string
      ([^Random rng s]
       (let [t (new-mutable-tick rng)]
         (a/go
           (loop [chars []
                  [c & more] s]
             (print (clojure.string/join chars))
             (flush)
             (a/<! (a/timeout (t)))
             (when c (recur (conj chars c) more))))))
      ([^Random rng s skew bias]
       (let [t (new-mutable-tick rng skew bias)]
         (a/go
           (loop [chars []
                  [c & more] s]
             (print (clojure.string/join chars))
             (flush)
             (a/<! (a/timeout (t)))
             (when c (recur (conj chars c) more)))))))
    (type-string
     (Random. (rand-int 1e4))
     "lorem ipsum dolor sit amet")))
