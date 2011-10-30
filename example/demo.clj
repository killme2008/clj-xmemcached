;;A demo for clj-xmemcached
(ns demo
  (:use [clj-xmemcached.core]))

(def client (xmemcached "localhost:11211"
						:name "demo"
						:pool 2
						:hash "consistent"
						:protocol "binary"))
;;Store items
(xset client "key" "dennis")
(xset client "key" "dennis" 100)
(xappend client "key" " zhuang")
(xprepend client "key" "hello,")

;;Get/Gets
(prn (xget client "key"))
(prn (xgets client "key"))

;;Incr/Decr numbers
(prn (xincr client "num" 1))
(prn (xdecr client "num" 1))
(prn  (xincr client "num" 1 0))

;;Bulk get items
(prn (xget client "key" "num"))

;;Delete items
(xdelete client "num")
(prn (xget client "num"))

;;Compare and set
(xset client "key" "hello")
(xcas client "key" #(str % " world"))
(prn (xget client "key"))
(xcas client "key" #(.hashCode %) 1)
(prn (xget client "key"))

;;stats/flush/shutdown
(prn (xstats client))
(xflush client)
(prn (xstats client))
(xshutdown client)



