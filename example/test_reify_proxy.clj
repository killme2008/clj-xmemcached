;;An example to test performance between reify and proxy
(ns test-reify-proxy
  (:use [clj-xmemcached.core]))

(def client (memcached "localhost:11211"))
(with-client client
  (set "key" 0)
  (time (dotimes [_ 10000]
          (cas "key"  inc)))
  (prn (get "key"))
  (shutdown))






