(ns clj-xmemcached.test.core
  (:use [clj-xmemcached.core])
  (:use [clojure.test])
  (:import (net.rubyeye.xmemcached MemcachedClient MemcachedClientBuilder XMemcachedClient CASOperation XMemcachedClientBuilder)
		   (net.rubyeye.xmemcached.impl KetamaMemcachedSessionLocator ArrayMemcachedSessionLocator)
		   (net.rubyeye.xmemcached.utils AddrUtil)
		   (net.rubyeye.xmemcached.command BinaryCommandFactory KestrelCommandFactory TextCommandFactory)
		   (java.net InetSocketAddress)))


(defmacro with-private-fns [[ns fns] & tests]
  "Refers private fns from ns and runs tests in context."
  `(let ~(reduce #(conj %1 %2 `(ns-resolve '~ns '~%2)) [] fns)
	 ~@tests))
;;Test memcached servers,plz configure it by yourself on your machine.
(def test-servers "localhost:11211")

(with-private-fns [clj-xmemcached.core [make-command-factory make-session-locator]]
  (deftest test-make-command-factory
	(is (instance? TextCommandFactory (make-command-factory "text")))
	(is (instance? BinaryCommandFactory (make-command-factory "binary")))
	(is (instance? KestrelCommandFactory (make-command-factory "kestrel"))))
  (deftest test-make-session-locator
	(is (instance? ArrayMemcachedSessionLocator (make-session-locator "standard")))
	(is (instance? KetamaMemcachedSessionLocator (make-session-locator "ketama")))
	(is (instance? KetamaMemcachedSessionLocator (make-session-locator "consistent")))))


(deftest test-xmemcached1
  (let [cli (xmemcached test-servers
						:protocol "kestrel"
						:name "test"
						:hash "ketama"
						:pool 10)]
	(try
	  (is (= 10 (.. cli getConnector getSessionSet size)))
	  (is (= "Kestrel" (.. cli getProtocol name)))
	  (is (= "test" (.getName cli)))
	  (finally
	   (xshutdown cli)))))

(deftest test-xmemcached2
  (let [cli (xmemcached test-servers
						:protocol "binary"
						:name "test"
						:hash "standard"
						:pool 2)]
	(try
	  (is (= 2 (.. cli getConnector getSessionSet size)))
	  (is (= "Binary" (.. cli getProtocol name)))
	  (is (= "test" (.getName cli)))
	  (finally
	   (xshutdown cli)))))

(deftest test-store-get-delete
  (let [cli (xmemcached test-servers)]
	(try
	  (is (xadd cli "a" 1))
	  (is (not (xadd cli "a" 2)))
	  (is (= 1 (xget cli "a")))
	  (is (xreplace cli "a" 2))
	  (is (= 2 (xget cli "a")))
	  (is (xset cli "a" 3))
	  (is (= 3 (xget cli "a")))
	  (is (xdelete cli "a"))
	  (is (nil? (xget cli "a")))
	  (finally
	   (xflush cli)
	   (xshutdown cli)))))

(deftest test-gets
  (let [cli (xmemcached test-servers)]
	(try
	  (is (xadd cli "a" 1))
	  (is (:value (xgets cli "a")))
	  (is (> (:cas (xgets cli "a")) 0))
	  (finally
	   (xflush cli)
	   (xshutdown cli)))))

(deftest test-expire
  (let [cli (xmemcached test-servers)]
	(try
	  (is (xadd cli "a" 1 1))
	  (is (= 1 (xget cli "a")))
	  (Thread/sleep 2000)
	  (is (nil? (xget cli "a")))
	  (finally
	   (xflush cli)
	   (xshutdown cli)))))

(deftest test-incr-decr
  (let [cli (xmemcached test-servers)]
	(try
	  (is (= 0 (xincr cli "a" 1)))
	  (is (= 2 (xincr cli "a" 2)))
	  (is (= 1 (xdecr cli "a" 1)))
	  (is (= "1" (xget cli "a")))
	  (finally
	   (xflush cli)
	   (xshutdown cli)))))

(deftest test-append-prepend
  (let [cli (xmemcached test-servers)]
	(try
	  (is (not (xappend cli "a" "hello")))
	  (is (xset cli "a" "dennis"))
	  (is (xappend cli "a" " zhuang"))
	  (is (= "dennis zhuang" (xget cli "a")))
	  (is (xprepend cli "a" "hello,"))
	  (is (= "hello,dennis zhuang" (xget cli "a")))
	  (finally
	   (xflush cli)
	   (xshutdown cli)))))

(deftest test-cas
  (let [cli (xmemcached test-servers)]
	(try
	  (is  (xset cli "key" "hello"))
	  (is (xcas cli "key" #(str % " world")))
	  (is (= "hello world"  (xget cli "key")))
	  (is (xcas cli "key" #(.hashCode %) 1))
	  (is (= (.hashCode "hello world") (xget cli "key")))
	  (finally
	   (xflush cli)
	   (xshutdown cli)))))



