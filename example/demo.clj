;;A demo for clj-xmemcached
(ns demo
  (:use [clj-xmemcached.core]))

(def client (xmemcached "localhost:12000"
						:name "demo"
						:pool 2
						:hash "consistent"
						:protocol "binary"))

(xset client "key" "dennis")
(xset client "key" "dennis" 100)
(xappend client "key" " zhuang")
(xprepend client "key" "hello,")

(prn (xget client "key"))
(prn (xgets client "key"))

(prn (xincr client "num" 1))
(prn (xdecr client "num" 1))
(prn  (xincr client "num" 1 0))

(xdelete client "num")
(prn (xget client "num"))

(xset client "key" "hello")
(xcas client "key" #(str % " world"))
(prn (xget client "key"))
(xcas client "key" #(.hashCode %) 1)
(prn (xget client "key"))

(prn (xstats client))
(xflush client)
(prn (xstats client))
(xshutdown client)



