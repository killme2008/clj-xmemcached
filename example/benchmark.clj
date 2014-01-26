(ns benchmark
  (:require [clj-xmemcached.core :as xm]))


(def client (xm/memcached "localhost:11211" :transcoder xm/nippy-transcoder))
(def threads 50)
(def repeats 10000)

(defmacro benchmark [doc & body]
  (println (str doc ": threads=" threads ",repeats=" repeats ",total=" (* threads repeats)))
  `(let [start# (System/nanoTime)
         ^java.util.concurrent.CyclicBarrier bar# (java.util.concurrent.CyclicBarrier. (inc threads))]
     (doseq [~'thread (range threads)]
       (.start ^Thread. (Thread.
        (fn []
          (.await bar#)
          (try (xm/with-client client
                 (doseq [~'times (range repeats)]
                   ~@body))
               (catch Exception e#
                   (.printStackTrace e#))
               (finally
                (.await bar#)))))))
     (.await bar#)
     (.await bar#)
     (println (str "Elapsed time: " (/ (double (- (. System (nanoTime)) start#)) 1000000.0) " msecs"))))

;;4K Bytes value
(def value (byte-array 4096))
(benchmark "Benchmark set"
           (xm/set (str (+ times (* thread repeats))) value))
(benchmark "Benchmark get"
           (xm/get (str (+ times (* thread repeats)))))
(benchmark "Benchmark set & get"
           (xm/set (str (+ times (* thread repeats))) value)
           (xm/get (str (+ times (* thread repeats)))))
(xm/shutdown @client)
