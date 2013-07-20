;;A demo for clj-xmemcached
(ns demo
  (:require [clj-xmemcached.core :as xm]))

(def client (xm/memcached "localhost:11211"
                           :name "demo"
                           :pool 2
                           :hash :ketama
                           :protocol :binary))

(defmacro wxm
  [& body]
  `(xm/with-client client ~@body))

(wxm
 ;;Store items
 (xm/set "key" "dennis")
 (xm/set "key" "dennis" 100)
 (xm/append "key" " zhuang")
 (xm/prepend "key" "hello,")

 ;;Get/Gets
 (prn (xm/get "key"))
 (prn (xm/gets "key"))

 ;;Incr/Decr numbers
 (prn (xm/incr "num" 1))
 (prn (xm/decr "num" 1))
 (prn (xm/incr "num" 1 0))

 ;;Bulk get items
 (prn (xm/get "key" "num"))

 ;;Delete items
 (xm/delete "num")
 (prn (xm/get "num"))
 (set "num" 1)
 ;;delete item with CAS,only valid in binary protocol.
 (xm/delete "num" (:cas (gets "num")))
 (prn (xm/get "num"))

 ;;Compare and set
 (xm/set "key" "hello")
 (xm/cas "key" #(str % " world"))
 (prn (xm/get  "key"))
 (xm/cas "key" #(.hashCode %) 1)
 (prn (xm/get "key"))

 ;;stats/flush/shutdown
 (prn (xm/stats))
 (xm/flush-all)
 (prn (xm/stats))
 (xm/shutdown))




